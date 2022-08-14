package com.atguigu.controller;


import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.config.AlipayConfig;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-09
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {
    @Autowired
    private PaymentInfoService paymentInfoService;
    //1.创建支付二维码
    @RequestMapping("createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.createQrCode(orderId);
    }

    //2.支付成功之后支付宝调用我们的地址
    @PostMapping("/async/notify")
    public String asyncNotify(@RequestParam Map<String,String> aliPayParam) throws Exception {
        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(aliPayParam, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        if(signVerified){
            //处理商户自身业务处理
            String tradeStatus = aliPayParam.get("trade_status");
            if("TRADE_SUCCESS".equals(tradeStatus)||"TRADE_FINISHED".equals(tradeStatus)){
                //查询支付表信息
                String outTradeNo = aliPayParam.get("out_trade_no");
                PaymentInfo paymentInfo=paymentInfoService.getPaymentInfo(outTradeNo);
                String paymentStatus = paymentInfo.getPaymentStatus();
                //如果支付表信息已经支付过或者关闭了 就不需要再执行了
                if(paymentStatus.equals(PaymentStatus.PAID.name())||paymentStatus.equals(PaymentStatus.ClOSED.name())){
                    return "success";
                }
                //修改支付表信息
                paymentInfoService.updatePaymentInfo(aliPayParam);
            }

        }else{
            return "failure";
        }
        return "failure";
    }

    //3.退款接口
    @GetMapping("refund/{orderId}")
    public boolean refund(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.refund(orderId);
    }
    //4.查询支付宝是否有交易记录接口
    @GetMapping("queryAlipayTrade/{orderId}")
    public boolean queryAlipayTrade(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.queryAlipayTrade(orderId);
    }
    //5.交易关闭
    @GetMapping("closeAlipayTrade/{orderId}")
    public boolean closeAlipayTrade(@PathVariable Long orderId) throws Exception {
        return paymentInfoService.closeAlipayTrade(orderId);
    }
    //6.根据outTradeNo查询支付表单信息
    @GetMapping("getPaymentInfo/{outTradeNo}")
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo) throws Exception {
        return paymentInfoService.getPaymentInfo(outTradeNo);
    }
}

