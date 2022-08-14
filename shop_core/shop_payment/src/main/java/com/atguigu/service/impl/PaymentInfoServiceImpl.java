package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.client.OrderFeignClient;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.mapper.PaymentInfoMapper;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-09
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Override
    public String createQrCode(Long orderId) throws Exception {
        //根据订单id查询订单信息
        OrderInfo order = orderFeignClient.getOrder(orderId);
        //保存支付订单信息表
        savePaymentInfo(order);


        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //支付成功之后的异步通知 支付宝调用我们的商户系统
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //支付成功之后的同步通知 支付成功之后跳转到商户系统指定页面
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();
        //商户订单号
        bizContent.put("out_trade_no", order.getOutTradeNo());
        //总金额
        bizContent.put("total_amount", order.getTotalMoney());
        bizContent.put("subject", "天气太热，买个锤子手机");

        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
            String alipayHtml=response.getBody();
            return alipayHtml;
        } else {
            System.out.println("调用失败");
        }
        return null;
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        wrapper.eq("payment_type",PaymentType.ALIPAY.name());
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void updatePaymentInfo(Map<String, String> aliPayParam) {
        String outTradeNo = aliPayParam.get("out_trade_no");
        PaymentInfo paymentInfo = getPaymentInfo(outTradeNo);
        //修改支付表信息为已支付
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        //以及整个支付传递的参数信息
        paymentInfo.setCallbackContent(aliPayParam.toString());
        //保存支付宝给的交易号
        String tradeNo = aliPayParam.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        baseMapper.updateById(paymentInfo);
        //发消息给shop-order修改订单状态
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE,MqConst.PAY_ORDER_ROUTE_KEY,paymentInfo.getOrderId());
    }

    @Override
    public boolean refund(Long orderId) throws AlipayApiException {
        OrderInfo order = orderFeignClient.getOrder(orderId);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_request_no", order.getOutTradeNo());
        bizContent.put("refund_amount", order.getTotalMoney());
        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            PaymentInfo paymentInfo = getPaymentInfo(order.getOutTradeNo());
            paymentInfo.setPaymentStatus(ProcessStatus.CLOSED.name());
            baseMapper.updateById(paymentInfo);
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
    private void savePaymentInfo(OrderInfo order) {
        //判断支付表单里面是否有添加过该记录
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",order.getId());
        wrapper.eq("payment_type",PaymentType.ALIPAY.name());
        Integer count = baseMapper.selectCount(wrapper);
        if(count>0){
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(order.getOutTradeNo());
        paymentInfo.setOrderId(order.getId()+"");
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setPaymentMoney(order.getTotalMoney());
        paymentInfo.setPaymentContent(order.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        baseMapper.insert(paymentInfo);

    }
    @Override
    public boolean queryAlipayTrade(Long orderId) throws AlipayApiException {
        OrderInfo orderInfo = orderFeignClient.getOrder(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean closeAlipayTrade(Long orderId) throws AlipayApiException {
        OrderInfo orderInfo = orderFeignClient.getOrder(orderId);
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }
    }
}
