package com.atguigu.executor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "thread.pool")
public class MyThreadPoolProperties {
    public Integer corePoolSize=16;
    public Integer maximumPoolSize=32;
    public Long keepAliveTime=50L;
    public Integer queueLength=100;
}
