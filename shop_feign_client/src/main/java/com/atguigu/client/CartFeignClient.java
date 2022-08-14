package com.atguigu.client;

import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-cart")
public interface CartFeignClient {
    //1.加入购物车
    @GetMapping("/cart/addCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId, @PathVariable Long skuNum);
    //5.查询勾选的购物车信息--送货清单  其他微服务调用
    @GetMapping("/cart/getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId);
}
