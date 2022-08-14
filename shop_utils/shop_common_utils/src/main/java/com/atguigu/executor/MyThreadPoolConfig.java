package com.atguigu.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Configuration
@EnableConfigurationProperties(MyThreadPoolProperties.class)
public class MyThreadPoolConfig {
    @Autowired
    private MyThreadPoolProperties myProperties;
    /**
     * 高并发情况下 150个线程进来
     *  16多少个线程会立即得到处理
     *  扩展16个线程去继续处理
     *  100个线程会进入到队列中去
     *  还剩余18个线程采用拒绝策略
     * 采用什么队列好些
     *  ArrayBlockingQueue
     *      空间碎片问题，导致内存空间不连续
     *  LinkedBlockingQueue
     *      不会引起空间碎片问题
     */
    @Bean
    public ThreadPoolExecutor myExecutor(){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                myProperties.getCorePoolSize(),
                myProperties.getMaximumPoolSize(),
                myProperties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue(myProperties.getQueueLength()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return threadPoolExecutor;
    }


}
