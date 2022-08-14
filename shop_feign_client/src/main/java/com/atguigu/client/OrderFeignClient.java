package com.atguigu.client;

import com.atguigu.entity.OrderInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-order")
public interface OrderFeignClient {
    //1.订单的确认信息
    @GetMapping("/order/confirm")
    public RetVal confirm();
    //2.根据订单id查询订单信息
    @GetMapping("/order/getOrder/{orderId}")
    public OrderInfo getOrder(@PathVariable Long orderId);
    //3.保存订单及详情
    @PostMapping("/order/saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo);
}
