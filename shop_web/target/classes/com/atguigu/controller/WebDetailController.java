package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
public class WebDetailController {
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor myPoolExecutor;

    @RequestMapping("{skuId}.html")
    public String getSkuDetail(@PathVariable Long skuId, Model model){
        //c.根据skuId查询商品的实时价格
        CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            System.out.println(Thread.currentThread().getName());
            model.addAttribute("price",skuPrice);
        },myPoolExecutor);

        //a.根据skuId查询商品的基本信息
        CompletableFuture<SkuInfo> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            model.addAttribute("skuInfo", skuInfo);
            System.out.println(Thread.currentThread().getName());
            return skuInfo;
        },myPoolExecutor);

        //b.根据三级分类id查询商品的分类
        CompletableFuture<Void> categoryViewFuture = skuInfoFuture.thenAcceptAsync((SkuInfo skuInfo) -> {
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            model.addAttribute("categoryView", categoryView);
            System.out.println(Thread.currentThread().getName());
        },myPoolExecutor);

        //d.销售属性id的组合与skuId的对应关系
        CompletableFuture<Void> salePropertyFuture = skuInfoFuture.thenAcceptAsync((SkuInfo skuInfo) -> {
            Long productId = skuInfo.getProductId();
            Map<Object, Object> salePropertyAndSkuIdMapping = productFeignClient.getSalePropertyAndSkuIdMapping(productId);
            model.addAttribute("salePropertyValueIdJson", JSON.toJSONString(salePropertyAndSkuIdMapping));
            System.out.println(Thread.currentThread().getName());
        },myPoolExecutor);

        //e.获取所有的销售属性(spu全份)和sku的销售属性(一份)
        CompletableFuture<Void> spuSalePropertyFuture = skuInfoFuture.thenAcceptAsync((SkuInfo skuInfo) -> {
            Long productId = skuInfo.getProductId();
            List<ProductSalePropertyKey> spuSalePropertyList = productFeignClient.getSpuSalePropertyAndSelected(productId, skuId);
            model.addAttribute("spuSalePropertyList",spuSalePropertyList);
            System.out.println(Thread.currentThread().getName());
        },myPoolExecutor);

        //代表每一个异步请求都要执行完才 返回到页面
        CompletableFuture.allOf(priceFuture,
                skuInfoFuture,
                categoryViewFuture,
                salePropertyFuture,
                spuSalePropertyFuture).join();
        return "detail/index";
    }


//    @RequestMapping("{skuId}.html")
//    public String getSkuDetail(@PathVariable Long skuId, Model model){
//        //a.根据skuId查询商品的基本信息
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        model.addAttribute("skuInfo",skuInfo);
//        //b.根据三级分类id查询商品的分类
//        Long category3Id = skuInfo.getCategory3Id();
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
//        model.addAttribute("categoryView",categoryView);
//        //c.根据skuId查询商品的实时价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        model.addAttribute("price",skuPrice);
//        //d.销售属性id的组合与skuId的对应关系
//        Long productId = skuInfo.getProductId();
//        Map<Object, Object> salePropertyAndSkuIdMapping = productFeignClient.getSalePropertyAndSkuIdMapping(productId);
//        model.addAttribute("salePropertyValueIdJson", JSON.toJSONString(salePropertyAndSkuIdMapping));
//        //e.获取所有的销售属性(spu全份)和sku的销售属性(一份)
//        List<ProductSalePropertyKey> spuSalePropertyList = productFeignClient.getSpuSalePropertyAndSelected(productId, skuId);
//        model.addAttribute("spuSalePropertyList",spuSalePropertyList);
//        return "detail/index";
//    }
}
