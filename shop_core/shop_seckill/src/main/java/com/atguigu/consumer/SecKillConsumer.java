package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Component
public class SecKillConsumer {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;

    //接受秒杀商品上架的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SCAN_SECKILL_EXCHANGE,durable = "false"),
            key = {MqConst.SCAN_SECKILL_ROUTE_KEY}
    ))
    public void scanSecKill() {
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //a.扫描已经通过审核的商品(秒杀商品)
        wrapper.eq("status",1);
        //b.秒杀商品数量num>0
        wrapper.gt("num",0);
        //c.取出当天日期的秒杀商品 select * from seckill_product where DATE_FORMAT(start_time,'%Y-%m-%d')='2022-08-12'
        wrapper.ge("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        //d.把秒杀商品放入到redis中
        if(!CollectionUtils.isEmpty(seckillProductList)){
            for (SeckillProduct seckillProduct : seckillProductList) {
                String skuId = seckillProduct.getSkuId().toString();
                //以hash结构存储信息
                redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId,seckillProduct);
                for (int i = 0; i <  seckillProduct.getNum(); i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+skuId).leftPush(skuId);
                }
                //f.通知其他节点秒杀状态位 生产者
                redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_SECKILL);
            }
        }
    }

    //2.消费秒杀预下单里面的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE,durable = "false"),
            key = {MqConst.PREPARE_SECKILL_ROUTE_KEY}
    ))
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo) {
        if(userSeckillSkuInfo!=null){
            //开始处理预下单逻辑
            seckillProductService.prepareSecKill(userSeckillSkuInfo);
        }
    }

    //3.秒杀商品的下架操作
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.CLEAR_REDIS_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.CLEAR_REDIS_EXCHANGE,durable = "false"),
            key = {MqConst.CLEAR_REDIS_ROUTE_KEY}
    ))
    public void clearSecKill() {
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //a.扫描已经通过审核的商品(秒杀商品)
        wrapper.eq("status",1);
        //b.秒杀商品的结束时间小于当前时间
        wrapper.le("end_time",new Date());
        List<SeckillProduct> seckillProductList = seckillProductService.list(wrapper);
        if(!CollectionUtils.isEmpty(seckillProductList)) {
            for (SeckillProduct seckillProduct : seckillProductList) {
                //把秒杀商品状态改为结束 2
                seckillProduct.setStatus("2");
                seckillProductService.updateById(seckillProduct);
                //c.将下架商品从redis中删除
                redisTemplate.delete(RedisConst.SECKILL_STATE_PREFIX+seckillProduct.getSkuId());
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillProduct.getSkuId());
                redisTemplate.delete(RedisConst.PREPARE_SECKILL_USERID_SKUID+"*");
            }
        }
        redisTemplate.delete(RedisConst.SECKILL_PRODUCT);
        redisTemplate.delete(RedisConst.PREPARE_SECKILL_USERID_ORDER);
        //把这个信息持久化到数据库里面--先持久
        redisTemplate.delete(RedisConst.BOUGHT_SECKILL_USER_ORDER);
    }
}
