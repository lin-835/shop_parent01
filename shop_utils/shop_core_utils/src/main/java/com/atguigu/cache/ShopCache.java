package com.atguigu.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//该注解只能用在那些地方
@Target({ElementType.TYPE, ElementType.METHOD})
//该注解的生命周期 在运行代码期间要用到该注解
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopCache {
    String value() default "cache";
    //定义一个缓存前缀 目的:该缓存属于哪个部分的缓存
    String prefix() default "cache";
    //是否需要开启布隆过滤器
    boolean enableBloom() default true;
}
