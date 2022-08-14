package com.atguigu.client;

import com.atguigu.entity.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-user")
public interface UserFeignClient {
    //1.根据用户id查询用户的收货地址
    @GetMapping("/user/getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId);
}
