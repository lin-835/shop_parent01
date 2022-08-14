package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.PaymentInfoService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PaymentConsumer {
    @Autowired
    private PaymentInfoService paymentInfoService;
    //1.关闭支付表单
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.CLOSE_PAYMENT_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.CLOSE_PAYMENT_EXCHANGE,durable = "false"),
            key = {MqConst.CLOSE_PAYMENT_ROUTE_KEY}
    ))
    public void updatePaymentAfterClose(String outTradeNo){
        if(!StringUtils.isEmpty(outTradeNo)){
            PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(outTradeNo);
            if(paymentInfo!=null){
                paymentInfo.setPaymentStatus(ProcessStatus.CLOSED.name());
                paymentInfoService.updateById(paymentInfo);
            }

        }
    }
}
