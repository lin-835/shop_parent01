package com.atguigu.cache;

import com.atguigu.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class ShopCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter skuBloomFilter;

    @Around("@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint target) throws Throwable {
        //1.需要从目标方法上去拿取skuId
        Object[] methodParams = target.getArgs();
        Object methodParam="";
        if(methodParams.length>0){
            methodParam=methodParams[0];
        }
        //通过target拿到目标方法
        MethodSignature methodSignature=(MethodSignature)target.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //通过目标方法拿到其上面的注解
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        //通过注解得到缓存key的前缀
        String prefix = shopCache.prefix();
        //得到是否开启布隆
        boolean enableBloom = shopCache.enableBloom();

        String cacheKey= prefix+":"+methodParam;
        //a.从缓存中查询数据
        Object cacheFromRedis = redisTemplate.opsForValue().get(cacheKey);
        if(cacheFromRedis==null){
            String lockKey="lock-"+methodParam;
            Object queryFromDb=new Object();
            synchronized (lockKey.intern()){
                queryFromDb = target.proceed();
                //c.放入数据到缓存
                redisTemplate.opsForValue().set(cacheKey, queryFromDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
            }
            return queryFromDb;
        }
        return cacheFromRedis;

    }

    //@Around("@annotation(com.atguigu.cache.ShopCache)")
    public Object cacheAroundAdvice2(ProceedingJoinPoint target) throws Throwable {
        //1.需要从目标方法上去拿取skuId
        Object[] methodParams = target.getArgs();
        Object methodParam="";
        if(methodParams.length>0){
            methodParam=methodParams[0];
        }
        //通过target拿到目标方法
        MethodSignature methodSignature=(MethodSignature)target.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //通过目标方法拿到其上面的注解
        ShopCache shopCache = targetMethod.getAnnotation(ShopCache.class);
        //通过注解得到缓存key的前缀
        String prefix = shopCache.prefix();
        //得到是否开启布隆
        boolean enableBloom = shopCache.enableBloom();

        String cacheKey= prefix+":"+methodParam;
        //a.从缓存中查询数据
        Object cacheFromRedis = redisTemplate.opsForValue().get(cacheKey);
        if(cacheFromRedis==null){
            String lockKey="lock-"+methodParam;
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock();
            try {
                Object queryFromDb=new Object();
                if(enableBloom){
                    //先去看布隆过滤器是否有该id
                    boolean flag =skuBloomFilter.contains(methodParam);
                    if(flag){
                        //b.如果缓存里面没有数据 从数据库中查
                        queryFromDb = target.proceed();
                    }
                }else{
                    queryFromDb = target.proceed();
                }
                //c.放入数据到缓存
                redisTemplate.opsForValue().set(cacheKey, queryFromDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                return queryFromDb;
            } finally {
                lock.unlock();
            }
        }
        return cacheFromRedis;
    }

    public static void main(String[] args) {
        String a = new String("49");
        String b = new String("49");
        System.out.println(a.intern()==b.intern());

        String c="49";
        String d="49";
        System.out.println(c==d);
    }
}
