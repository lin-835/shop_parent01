package com.atguigu.service.impl;

import com.atguigu.cache.ShopCache;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.exception.SleepUtils;
import com.atguigu.mapper.ProductSalePropertyKeyMapper;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SkuDetailServiceImpl implements SkuDetailService {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImageService skuImageService;
    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;
    @Autowired
    private ProductSalePropertyKeyMapper salePropertyKeyMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter<Long> skuBloomFilter;

    @ShopCache(prefix="skuInfo")
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        SkuInfo skuInfo = getSkuInfoFromDB(skuId);
        return skuInfo;
    }

    //SkuInfo skuInfo = getSkuInfoFromRedis(skuId);
    //SkuInfo skuInfo=getSkuInfoFromRedisWithThreadLocal(skuId);
    //SkuInfo skuInfo=getSkuInfoFromRedisson(skuId);
    //利用redisson实现分布式锁并拿到数据
    private SkuInfo getSkuInfoFromRedisson(Long skuId) {
        //sku:24:info
        String cacheKey= RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
        //a.从缓存中查询数据
        SkuInfo skuInfoRedis = (SkuInfo)redisTemplate.opsForValue().get(cacheKey);
        if(skuInfoRedis==null){
            String lockKey="lock-"+skuId;
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock();
            try {
                //先去看布隆过滤器是否有该id
                boolean flag = skuBloomFilter.contains(skuId);
                SkuInfo skuInfoDb=new SkuInfo();
                if(flag){
                    //b.如果缓存里面没有数据 从数据库中查
                    skuInfoDb = getSkuInfoFromDB(skuId);
                }
                //c.放入数据到缓存
                redisTemplate.opsForValue().set(cacheKey, skuInfoDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                return skuInfoDb;
            } finally {
                lock.unlock();
            }

        }
        return skuInfoRedis;
    }

    //利用redis+lua+threadLocal查询商品基本信息
    public ThreadLocal<String> threadLocal = new ThreadLocal<>();
    private SkuInfo getSkuInfoFromRedisWithThreadLocal(Long skuId) {
        String cacheKey= RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
        //a.从缓存中查询数据
        SkuInfo skuInfoRedis = (SkuInfo)redisTemplate.opsForValue().get(cacheKey);
        if(skuInfoRedis==null){
            //还有很多操作要执行 去查缓存 去判断 去过滤等操作 200行代码要执行
            String token = threadLocal.get();
            boolean accquireLock = false;
            //定义一个锁的名称 减小锁的粒度
            String lockKey="lock-"+skuId;
            //代表第一次来 不参与自旋
            if (token == null) {
                token = UUID.randomUUID().toString();
                accquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.MINUTES);
            } else {
                //代表已经拿到锁了
                accquireLock = true;
            }
            if (accquireLock) {
                SkuInfo skuInfoDB = doBusiness(skuId, cacheKey);
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptText(luaScript);
                redisScript.setResultType(Long.class);
                redisTemplate.execute(redisScript, Arrays.asList(lockKey), token);
                //擦屁股 防止内存泄漏
                threadLocal.remove();
                return skuInfoDB;
            } else {
                //自旋 只做一件事情 拿锁
                while (true) {
                    SleepUtils.millis(50);
                    //重试拿锁
                    boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.MINUTES);
                    if (retryAccquireLock) {
                        //拿到锁以后就不要去自旋了
                        threadLocal.set(token);
                        break;
                    }
                }
               return getSkuInfoFromRedisWithThreadLocal(skuId);
            }
        }else{
            return skuInfoRedis;
        }
    }

    private SkuInfo doBusiness(Long skuId, String cacheKey) {
        //b.如果缓存里面没有数据 从数据库中查
        SkuInfo skuInfoDb = getSkuInfoFromDB(skuId);
        //c.放入数据到缓存
        redisTemplate.opsForValue().set(cacheKey, skuInfoDb, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
        return skuInfoDb;
    }

    //从redis中获取商品基本信息
    private SkuInfo getSkuInfoFromRedis(Long skuId) {
        //sku:24:info
        String cacheKey= RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
        //a.从缓存中查询数据
        SkuInfo skuInfoRedis = (SkuInfo)redisTemplate.opsForValue().get(cacheKey);
        if(skuInfoRedis==null){
            //b.如果缓存里面没有数据 从数据库中查
            return doBusiness(skuId, cacheKey);
        }
        return skuInfoRedis;
    }

    private SkuInfo getSkuInfoFromDB(Long skuId) {
        //1.查询商品基本信息
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        //2.查询商品图片信息
        if (skuInfo != null) {
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id", skuId);
            List<SkuImage> skuImageList = skuImageService.list(wrapper);
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    public Map<Object, Object> getSalePropertyAndSkuIdMapping(Long productId) {
        Map<Object, Object> salePropertyAndSkuIdMap = new HashMap<>();
        List<Map> retListMap=skuSalePropertyValueService.getSalePropertyAndSkuIdMapping(productId);
        for (Map retMap : retListMap) {
            salePropertyAndSkuIdMap.put(retMap.get("sale_property_value_id"),retMap.get("sku_id"));
        }
        return salePropertyAndSkuIdMap;
    }

    @Override
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId, Long skuId) {
        return salePropertyKeyMapper.getSpuSalePropertyAndSelected(productId,skuId);
    }
}
