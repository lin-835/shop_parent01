package com.atguigu.controller;


import com.atguigu.client.CartFeignClient;
import com.atguigu.client.UserFeignClient;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-06
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private OrderInfoService orderInfoService;
    //1.订单的确认信息
    @GetMapping("confirm")
    public RetVal confirm(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //用户收货地址信息 shop-user
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        //订单的送货清单 shop-cart
        List<CartInfo> selectedCartInfoList = cartFeignClient.getSelectedCartInfo(userId);
        //商品的总件数与总金额
        List orderDetailList=new ArrayList<OrderDetail>();
        int totalNum=0;
        BigDecimal totalMoney = new BigDecimal(0);
        if(!CollectionUtils.isEmpty(selectedCartInfoList)){
            for (CartInfo cartInfo : selectedCartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum()+"");
                //订单的价格 不拿实时价格
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                //订单总金额
                totalMoney=totalMoney.add(cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                //总数量
                totalNum+=cartInfo.getSkuNum();
                orderDetailList.add(orderDetail);
            }
        }
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList",userAddressList);
        retMap.put("detailArrayList",orderDetailList);
        retMap.put("totalMoney",totalMoney);
        retMap.put("totalNum",totalNum);
        //生成一个流水号
        String tradeNo=orderInfoService.generateTradeNo(userId);
        retMap.put("tradeNo",tradeNo);
        return RetVal.ok(retMap);
    }
    //2.提交订单信息  http://api.gmall.com/order/submitOrder?tradeNo=xxxx
    @PostMapping("submitOrder")
    public RetVal submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //获取页面传递过来的tradeNo
        String tradeNoUI = request.getParameter("tradeNo");
        //同redis里的tradeNo进行对比
        boolean flag=orderInfoService.compareTradeNo(userId,tradeNoUI);
        if(!flag){
            return RetVal.fail().message("不能重复提交订单信息");
        }
        //验证商品库存与价格
        String warnningMessage=orderInfoService.checkStockAndPrice(userId,orderInfo);
        if(!StringUtils.isEmpty(warnningMessage)){
            return RetVal.fail().message(warnningMessage);
        }
        //保存订单基本信息和详情信息
        Long orderId=orderInfoService.saveOrderAndDetail(orderInfo);
        //订单提交成功之后还需要删除流水号
        orderInfoService.deleteTradeNo(userId);
        return RetVal.ok(orderId);
    }

    //3.根据订单id查询订单信息
    @GetMapping("getOrder/{orderId}")
    public OrderInfo getOrder(@PathVariable Long orderId) {
        return orderInfoService.getById(orderId);
    }

    //4.拆单接口 http://localhost:8004/order/splitOrder
    @PostMapping("splitOrder")
    public String splitOrder(@RequestParam Long orderId,@RequestParam String wareHouseIdSkuIdMapJson){
        return orderInfoService.splitOrder(orderId,wareHouseIdSkuIdMapJson);
    }
    //5.保存订单及详情
    @PostMapping("saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo){
        return orderInfoService.saveOrderAndDetail(orderInfo);
    }


}

