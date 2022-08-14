package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-06
 */
public interface OrderInfoService extends IService<OrderInfo> {

    Long saveOrderAndDetail(OrderInfo orderInfo);

    String generateTradeNo(String userId);

    boolean compareTradeNo(String userId, String tradeNoUI);

    void deleteTradeNo(String userId);

    String checkStockAndPrice(String userId, OrderInfo orderInfo);

    OrderInfo getOrderInfoAndDetail(Long orderId);

    void updateOrderStatus(OrderInfo orderInfo, ProcessStatus status);

    void sendMsgToWareHouse(OrderInfo orderInfo);

    String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson);
}
