package com.atguigu.controller;


import com.atguigu.entity.TOrder;
import com.atguigu.entity.TOrderDetail;
import com.atguigu.mapper.TOrder1Mapper;
import com.atguigu.service.TOrder1Service;
import com.atguigu.service.TOrderDetail1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-09
 */
@RestController
@RequestMapping("/order")
public class TOrder1Controller {
    @Autowired
    private TOrder1Service orderService;
    @Autowired
    private TOrderDetail1Service orderDetailService;

    @Autowired
    private TOrder1Mapper orderMapper;
    //1.保存订单分库分表
    @GetMapping("test01")
    public void test01(){
        for (int i = 0; i <10 ; i++) {
            TOrder order = new TOrder();
            order.setOrderPrice(99);
            String uuid = UUID.randomUUID().toString();
            order.setTradeNo(uuid);
            order.setOrderStatus("未支付");
            //分库分表依据
            int userId = new Random().nextInt(20);
            System.out.println("分库分表依据:"+userId);
            order.setUserId(Long.parseLong(userId+""));
            orderService.save(order);
        }
    }

    //2.保存订单与订单详情
    @GetMapping("test02")
    public void test02(){
        TOrder tOrder = new TOrder();
        tOrder.setTradeNo("enjoy6288");
        tOrder.setOrderPrice(9900);
        tOrder.setUserId(3L); //ds-1 table-4
        orderService.save(tOrder);

        TOrderDetail iphone13 = new TOrderDetail();
        iphone13.setOrderId(tOrder.getId());
        iphone13.setSkuName("Iphone13");
        iphone13.setSkuNum(1);
        iphone13.setSkuPrice(6000);
        iphone13.setUserId(3L); //要进行分片计算
        orderDetailService.save(iphone13);

        TOrderDetail sanxin = new TOrderDetail();
        sanxin.setOrderId(tOrder.getId());
        sanxin.setSkuName("三星");
        sanxin.setSkuNum(2);
        sanxin.setSkuPrice(3900);
        sanxin.setUserId(3L); //要进行分片计算
        orderDetailService.save(sanxin);
        System.out.println("保存完成....");
    }
    //3.查询订单与订单详情
    @GetMapping("test03")
    public void test03(){
        List<TOrder> orderList=orderMapper.queryOrderByCondition(3L,null);
        //不带分片键会查询全库
        //List<TOrder> orderList=orderMapper.queryOrderByCondition(null,1556845815206412289L);
        for (TOrder order : orderList) {
            System.out.println(order);
        }
    }

}

