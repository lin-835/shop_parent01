package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.service.SearchService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EsConsumer {
    @Autowired
    private SearchService searchService;
    //接受上架消息，默认采用自动签收
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.ON_SALE_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
            key = {MqConst.ON_SALE_ROUTING_KEY}
    ))
    public void onsale(Long skuId, Channel channel, Message message) throws Exception{
        if(skuId!=null){
            searchService.onSale(skuId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
    //接受下架消息，默认采用自动签收
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.OFF_SALE_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE,durable = "false"),
            key = {MqConst.OFF_SALE_ROUTING_KEY}
    ))
    public void offSale(Long skuId,Channel channel, Message message) throws Exception{
        if(skuId!=null){
            searchService.offSale(skuId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
