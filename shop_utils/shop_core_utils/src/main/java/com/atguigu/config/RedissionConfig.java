package com.atguigu.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissionConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        //可以用"rediss://"来启用SSL连接
        config.useSingleServer().setAddress("redis://192.168.18.208:6389");
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
