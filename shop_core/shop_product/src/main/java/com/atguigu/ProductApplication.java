package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients
//强扫
//@Import(MybatisPlusConfig.class)
public class ProductApplication {
   public static void main(String[] args) {
      SpringApplication.run(ProductApplication.class, args);
   }
}