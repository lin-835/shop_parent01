package com.atguigu.client;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-seckill")
public interface SecKillFeignClient {
    //1.秒杀商品列表显示
    @GetMapping("/seckill/queryAllSecKillProduct")
    public List<SeckillProduct> queryAllSecKillProduct();
    //2.单个秒杀商品详情
    @GetMapping("/seckill/querySecKillProductById/{skuId}")
    public SeckillProduct querySecKillProductById(@PathVariable Long skuId);

    //3.返回秒杀页面需要的数据
    @GetMapping("/seckill/seckillConfirm")
    public RetVal seckillConfirm();
}
