package com.atguigu.controller;


import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-12
 */
@RestController
@RequestMapping
public class SimulateQuartzController {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //1.发送一个上架秒杀商品的消息
    @GetMapping("sendMsgToScanSecKill")
    public String sendMsgToScanSecKill(){
        //发送消息只是起到一个通知的效果
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.SCAN_SECKILL_ROUTE_KEY,"xxx");
        return "success";
    }

    //2.发送一个下架秒杀商品的消息
    @GetMapping("sendMsgToClearSecKill")
    public String sendMsgToClearSecKill(){
        //发送消息只是起到一个通知的效果
        rabbitTemplate.convertAndSend(MqConst.CLEAR_REDIS_EXCHANGE,MqConst.CLEAR_REDIS_ROUTE_KEY,"xxx");
        return "success";
    }
}

