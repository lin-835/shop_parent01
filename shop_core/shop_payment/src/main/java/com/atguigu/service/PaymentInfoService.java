package com.atguigu.service;

import com.alipay.api.AlipayApiException;
import com.atguigu.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-09
 */
public interface PaymentInfoService extends IService<PaymentInfo> {

    String createQrCode(Long orderId) throws Exception;

    PaymentInfo getPaymentInfo(String outTradeNo);

    void updatePaymentInfo(Map<String, String> aliPayParam);

    boolean refund(Long orderId) throws AlipayApiException;

    boolean queryAlipayTrade(Long orderId) throws AlipayApiException;

    boolean closeAlipayTrade(Long orderId) throws AlipayApiException;
}
