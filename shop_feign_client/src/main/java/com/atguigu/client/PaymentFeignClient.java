package com.atguigu.client;

import com.atguigu.entity.PaymentInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-payment")
public interface PaymentFeignClient {
    //3.退款接口
    @GetMapping("/payment/refund/{orderId}")
    public boolean refund(@PathVariable Long orderId) ;
    //4.查询支付宝是否有交易记录接口
    @GetMapping("/payment/queryAlipayTrade/{orderId}")
    public boolean queryAlipayTrade(@PathVariable Long orderId);
    //5.交易关闭
    @GetMapping("/payment/closeAlipayTrade/{orderId}")
    public boolean closeAlipayTrade(@PathVariable Long orderId);
    //6.根据outTradeNo查询支付表单信息
    @GetMapping("/payment/getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);
}
