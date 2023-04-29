package com.atguigu.controller;

import com.atguigu.client.OrderFeignClient;
import com.atguigu.entity.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebPayController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    //1.跳转到支付页面
    @RequestMapping("pay.html")
    public String pay(Long orderId,Model model){
        //获取订单信息
        OrderInfo orderInfo = orderFeignClient.getOrder(orderId);
        model.addAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }

    //2.支付成功之后的跳转页面
    @RequestMapping("alipay/success.html")
    public String success(){
        //在这里还要写很多代码--修改订单状态,减少库存
        return "payment/success";
    }



}
