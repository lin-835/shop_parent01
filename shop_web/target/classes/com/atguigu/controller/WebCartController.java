package com.atguigu.controller;

import com.atguigu.client.CartFeignClient;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebCartController {
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private ProductFeignClient productFeignClient;
    //1.加入购物车 ?skuId=24&skuNum=2
    @RequestMapping("addCart.html")
    public String addCart(@RequestParam Long skuId, @RequestParam Long skuNum, Model model){
        //通过远程RPC调用shop-cart实现添加购物车
        cartFeignClient.addCart(skuId,skuNum);

        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        model.addAttribute("skuInfo",skuInfo);
        model.addAttribute("skuNum",skuNum);
        return "cart/addCart";
    }
    //2.购物车列表
    @RequestMapping("cart.html")
    public String cart(){
        return "cart/index";
    }


}
