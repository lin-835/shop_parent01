package com.atguigu.config;

import com.atguigu.constant.RedisConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
//这个你可以理解为消费者
@Component
public class ShopMessageReceiver {
    @Autowired
    private RedisTemplate redisTemplate;


    //把redis发送过来的消息进行一个处理--设置秒杀状态位
    public void receiveChannelMessage(String message){
        //""24:1""  seckill:state:24
        message = message.replaceAll("\"", "");
        String[] splitMessage = message.split(":");
        if(splitMessage.length==2){
            //splitMessage[0]是商品的skuId,splitMessage[1]是秒杀状态位
            redisTemplate.opsForValue().set(RedisConst.SECKILL_STATE_PREFIX+splitMessage[0],splitMessage[1]);
        }
    }
}
