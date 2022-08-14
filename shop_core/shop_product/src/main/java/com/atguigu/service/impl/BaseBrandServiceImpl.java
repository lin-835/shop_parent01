package com.atguigu.service.impl;

import com.atguigu.entity.BaseBrand;
import com.atguigu.exception.SleepUtils;
import com.atguigu.mapper.BaseBrandMapper;
import com.atguigu.service.BaseBrandService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 品牌表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-22
 */
@Service
public class BaseBrandServiceImpl extends ServiceImpl<BaseBrandMapper, BaseBrand> implements BaseBrandService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    //@Override
    public void setNum00() {
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        }
    }

    //@Override
    public synchronized void setNum1() {
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        }
    }

    private void doBusiness() {
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num", String.valueOf(++num));
        }
    }

    //分布式锁解决方案一
    //@Override
    public synchronized void setNum01() {
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", "ok");
        if (accquireLock) {
            //拿到锁 就可以执行业务 如果doBusiness出现异常 可能导致锁一直占用 无法释放
            doBusiness();
            //业务做完了需要删除锁
            redisTemplate.delete("lock");
        } else {
            //如果拿不到锁 就递归
            setNum();
        }
    }

    //分布式锁解决方案二
    //@Override
    public synchronized void setNum02() {
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", "ok", 3, TimeUnit.SECONDS);
        if (accquireLock) {
            //拿到锁 就可以执行业务 如果doBusiness出现异常 可能导致锁一直占用 无法释放
            doBusiness();
            //业务做完了需要删除锁
            redisTemplate.delete("lock");
        } else {
            //如果拿不到锁 就递归
            setNum();
        }
    }

    //分布式锁解决方案三
    //@Override
    public synchronized void setNum03() {
        String token = UUID.randomUUID().toString();
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
        if (accquireLock) {
            //拿到锁 就可以执行业务 如果doBusiness出现异常 可能导致锁一直占用 无法释放
            doBusiness();
            //业务做完了需要删除锁
            String redisToken = (String) redisTemplate.opsForValue().get("lock");
            if (token.equals(redisToken)) {
                redisTemplate.delete("lock");
            }
        } else {
            //如果拿不到锁 就递归
            setNum();
        }
    }

    //分布式锁解决方案四
    //@Override
    public void setNum04() {
        String token = UUID.randomUUID().toString();
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
        if (accquireLock) {
            //拿到锁 就可以执行业务 如果doBusiness出现异常 可能导致锁一直占用 无法释放
            doBusiness();
            //定义一个lua脚本
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //把上面脚本塞到redisScript里面
            redisScript.setScriptText(luaScript);
            //设置执行完脚本返回类型
            redisScript.setResultType(Long.class);
            //1.脚本对象 2.需要传递的keys 3.对比的值
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
            /**
             * 业务做完了需要删除锁
             * String redisToken =(String) redisTemplate.opsForValue().get("lock");
             *             if(token.equals(redisToken)){
             *                 redisTemplate.delete("lock");
             *             }
             */
        } else {
            //如果拿不到锁 就递归
            setNum();
        }
    }

    //分布式锁优化
    //@Override
    public void setNum05() {
        //还有很多操作要执行 去查缓存 去判断 去过滤等操作 200行代码要执行
        String token = UUID.randomUUID().toString();
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
        if (accquireLock) {
            doBusiness();
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
        } else {
            //自旋 只做一件事情 拿锁
            while (true) {
                SleepUtils.millis(50);
                //重试拿锁
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
                if (retryAccquireLock) {
                    //拿到锁以后就不要去自旋了
                    break;
                }
            }
            setNum();
        }
    }

    //分布式锁优化--重入锁的设计
    public Map<Thread, Boolean> threadMap = new HashMap();

    //@Override
    public void setNum06() {
        //还有很多操作要执行 去查缓存 去判断 去过滤等操作 200行代码要执行
        Boolean flag = threadMap.get(Thread.currentThread());
        boolean accquireLock = false;
        String token = null;
        //代表第一次来
        if (flag == null || flag == false) {
            token = UUID.randomUUID().toString();
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
        } else {
            //代表已经拿到锁了
            accquireLock = true;
        }
        if (accquireLock) {
            doBusiness();
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
        } else {
            //自旋 只做一件事情 拿锁
            while (true) {
                SleepUtils.millis(50);
                //重试拿锁
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
                if (retryAccquireLock) {
                    //拿到锁以后就不要去自旋了
                    threadMap.put(Thread.currentThread(), true);
                    break;
                }
            }
            setNum();
        }
    }


    //分布式锁优化--重入锁的设计
    /**
     * 内存泄漏
     * 你们线上都有遇到过哪些问题
     * 线上遇到过内存泄漏问题
     * a.通过线上日志以及抓取当前应用的内存模型
     * b.jvisualvm连上应用程序进行观测 发现有个map在不断地进行上涨
     */
    public Map<Thread, String> threadMap1 = new HashMap();

    //@Override
    public void setNum07() {
        //还有很多操作要执行 去查缓存 去判断 去过滤等操作 200行代码要执行
        String token = threadMap1.get(Thread.currentThread());
        boolean accquireLock = false;
        //代表第一次来 不参与自旋
        if (token == null) {
            token = UUID.randomUUID().toString();
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
        } else {
            //代表已经拿到锁了
            accquireLock = true;
        }
        if (accquireLock) {
            doBusiness();
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
            //擦屁股 防止内存泄漏
            threadMap1.remove(Thread.currentThread());
        } else {
            //自旋 只做一件事情 拿锁
            while (true) {
                SleepUtils.millis(50);
                //重试拿锁
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
                if (retryAccquireLock) {
                    //拿到锁以后就不要去自旋了
                    threadMap1.put(Thread.currentThread(), token);
                    break;
                }
            }
            setNum();
        }
    }

    public ThreadLocal<String> threadLocal = new ThreadLocal<>();
    //@Override
    public void setNum08() {
        //还有很多操作要执行 去查缓存 去判断 去过滤等操作 200行代码要执行
        String token = threadLocal.get();
        boolean accquireLock = false;
        //代表第一次来 不参与自旋
        if (token == null) {
            token = UUID.randomUUID().toString();
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
        } else {
            //代表已经拿到锁了
            accquireLock = true;
        }
        if (accquireLock) {
            doBusiness();
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
            //擦屁股 防止内存泄漏
            threadLocal.remove();
        } else {
            //自旋 只做一件事情 拿锁
            while (true) {
                SleepUtils.millis(50);
                //重试拿锁
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.MINUTES);
                if (retryAccquireLock) {
                    //拿到锁以后就不要去自旋了
                    threadLocal.set(token);
                    break;
                }
            }
            setNum();
        }
    }

    @Override
    public void setNum() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        try {
            doBusiness();
        } finally {
            lock.unlock();
        }
    }



}
