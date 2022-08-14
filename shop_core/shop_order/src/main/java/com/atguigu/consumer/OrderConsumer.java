package com.atguigu.consumer;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.PaymentFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderInfoService;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    //1.超时未支付自动取消订单
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void cancelOrder(Long orderId){
        //a.把订单改为已关闭
        OrderInfo orderInfo = orderInfoService.getById(orderId);
        orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
        orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());
        orderInfoService.updateById(orderInfo);
        //b.把支付表状态也改为关闭状态
        rabbitTemplate.convertAndSend(MqConst.CLOSE_PAYMENT_EXCHANGE,MqConst.CLOSE_PAYMENT_ROUTE_KEY,orderInfo.getOutTradeNo());
        //c.如果支付宝那边有交易记录也需要关闭
        boolean flag = paymentFeignClient.queryAlipayTrade(orderId);
        if(flag){
            paymentFeignClient.closeAlipayTrade(orderId);
        }
    }
    //2.支付成功之后修改订单状态
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PAY_ORDER_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE,durable = "false"),
            key = {MqConst.PAY_ORDER_ROUTE_KEY}
    ))
    public void updateOrderAfterPaySuccess(Long orderId){
        if(orderId!=null){
            //查询订单基本信息与详情信息是为了接下来减库存使用
            OrderInfo orderInfo=orderInfoService.getOrderInfoAndDetail(orderId);
            //当订单状态为未支付的时候才能修改
            if(orderInfo!=null&&orderInfo.getProcessStatus().equals(ProcessStatus.UNPAID.name())){
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.PAID);
                //通知仓库系统减库存
                orderInfoService.sendMsgToWareHouse(orderInfo);
            }
        }
        //手动签收
    }

    //3.仓库系统减库存成功之后的代码
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE,durable = "false"),
            key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}
    ))
    public void updateOrderAfterDecraseStock(String jsonData){
        if(!StringUtils.isEmpty(jsonData)){
            //把json转换为map
            Map<String, Object> map = JSON.parseObject(jsonData);
            String orderId=(String)map.get("orderId");
            String status=(String)map.get("status");
            //如果仓库系统减库存成功 把订单状态改为等待发货
            OrderInfo orderInfo = orderInfoService.getOrderInfoAndDetail(Long.parseLong(orderId));
            if("DEDUCTED".equals(status)){
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.WAITING_DELEVER);
            }else{
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.STOCK_EXCEPTION);
            }
        }
    }
}
