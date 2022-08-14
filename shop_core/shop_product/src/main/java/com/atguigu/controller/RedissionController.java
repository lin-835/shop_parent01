package com.atguigu.controller;


import com.atguigu.exception.SleepUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-22
 */
@RestController
@RequestMapping("/product")
public class RedissionController {
    @Autowired
    private RedissonClient redissonClient;
    /**
     * 1.可重入锁(最基本的锁)
     *  a.默认有个30秒的过期时间   internalLockLeaseTime=lockWatchdogTimeout= 30 * 1000
     *  b.每隔10秒钟续期          internalLockLeaseTime / 3
     *  c.看门狗机制
     *      Redisson实例被关闭前，不断的延长锁的有效期
     */
    @GetMapping("lock1")
    public String lock1() {
        RLock lock = redissonClient.getLock("lock");
        //加锁 一直等到拿锁成功 默认加锁方式
        String uuid=null;
        lock.lock();
        try {
            uuid = UUID.randomUUID().toString();
            SleepUtils.sleep(2);
            System.out.println(Thread.currentThread().getName()+"执行业务"+uuid);
        } finally {
            lock.unlock();
        }
        return Thread.currentThread().getName()+"执行业务"+uuid;
    }

    @GetMapping("lock2")
    public String lock2() {
        RLock lock = redissonClient.getLock("lock");
        String uuid=null;
        //加锁以后10秒钟自动解锁 不会有自动续期 无需调用unlock方法手动解锁 不怎么采用该方式
        lock.lock(10, TimeUnit.SECONDS);
        try {
            uuid = UUID.randomUUID().toString();
            SleepUtils.sleep(20);
            System.out.println(Thread.currentThread().getName()+"执行业务"+uuid);
        } finally {
            //这个代码无需调用
            //lock.unlock();
        }
        return Thread.currentThread().getName()+"执行业务"+uuid;
    }

    @GetMapping("lock3")
    public String lock3() {
        RLock lock = redissonClient.getLock("lock");
        String uuid=null;
        //尝试加锁，最大持有锁的时间是20，释放锁的时间是15
        try {
            boolean flag=lock.tryLock(20,15, TimeUnit.SECONDS);
            System.out.println(flag);
            uuid = UUID.randomUUID().toString();
            SleepUtils.sleep(25);
            System.out.println(Thread.currentThread().getName()+"执行业务"+uuid);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return Thread.currentThread().getName()+"执行业务"+uuid;
    }

    public String uuid=null;
    //测试写锁
    @GetMapping("write")
    public String write() {
        ReadWriteLock rwLock = redissonClient.getReadWriteLock("rwlock");
        Lock writeLock = rwLock.writeLock();
        //尝试加锁，最大持有锁的时间是20，释放锁的时间是15
        try {
            writeLock.lock();
            uuid = UUID.randomUUID().toString();
            SleepUtils.sleep(10);
            System.out.println(Thread.currentThread().getName()+"执行业务"+uuid);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
        return Thread.currentThread().getName()+"执行业务"+uuid;
    }

    //测试读锁
    @GetMapping("read")
    public String read() {
        ReadWriteLock rwLock = redissonClient.getReadWriteLock("rwlock");
        Lock readLock = rwLock.readLock();
        //尝试加锁，最大持有锁的时间是20，释放锁的时间是15
        try {
            readLock.lock();
            return uuid;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 测试信号量(Semaphore)
     * 停车场5个车位
     * a.占用车位
     * b.开走 释放车位
     */
    @GetMapping("park")
    public String park() throws Exception {
        RSemaphore parkStation = redissonClient.getSemaphore("park_station");
        //信号量减1
        parkStation.acquire(1);
        System.out.println(Thread.currentThread().getName()+"找到车位");
        return Thread.currentThread().getName()+"找到车位";
    }
    //开走
    @GetMapping("left")
    public String left() throws Exception {
        RSemaphore parkStation = redissonClient.getSemaphore("park_station");
        //信号量加1
        parkStation.release(1);
        System.out.println(Thread.currentThread().getName()+"left");
        return Thread.currentThread().getName()+"left";
    }

    @GetMapping("leftClassRoom")
    public String leftClassRoom() throws Exception {
        RCountDownLatch leftClass = redissonClient.getCountDownLatch("left_class");
        //如果有人走了 数量减1
        leftClass.countDown();
        return Thread.currentThread().getName()+"学员离开";
    }

    @GetMapping("lockDoor")
    public String lockDoor() throws Exception {
        RCountDownLatch leftClass = redissonClient.getCountDownLatch("left_class");
        //所有的人走了 班长才能走
        leftClass.trySetCount(6);
        leftClass.await();
        return Thread.currentThread().getName()+"班长开心的离开";
    }



}

