package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {
    @Autowired
    private RedissonClient redissonClient;
    //TODO 思考如何同步布隆过滤器与数据库信息
    @Bean
    public RBloomFilter<Long> skuBloomFilter(){
        //指定布隆过滤器的名称
        RBloomFilter<Long> bloomFilter= redissonClient.getBloomFilter(RedisConst.BLOOM_SKU_ID);
        //初始化布隆过滤器容器大小 期望存储精度是多少
        bloomFilter.tryInit(10000,0.001);
        return bloomFilter;
    }
}
