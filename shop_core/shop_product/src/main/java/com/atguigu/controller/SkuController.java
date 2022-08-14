package com.atguigu.controller;


import com.atguigu.client.SearchFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 库存单元表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@RestController
@RequestMapping("/product")
public class SkuController {
    @Autowired
    private ProductImageService productImageService;
    @Autowired
    private ProductSalePropertyKeyService salePropertyKeyService;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SkuInfoService skuInfoService;
    //1.根据spuId查询所有的spu图片 http://127.0.0.1/product/queryProductImageByProductId/12
    @GetMapping("queryProductImageByProductId/{spuId}")
    public RetVal queryProductImageByProductId(@PathVariable Long spuId) {
        QueryWrapper<ProductImage> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id",spuId);
        List<ProductImage> productImageList = productImageService.list(wrapper);
        return RetVal.ok(productImageList);
    }
    //2.根据spuId查询所有的销售属性 http://127.0.0.1/product/querySalePropertyByProductId/12
    @GetMapping("querySalePropertyByProductId/{spuId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long spuId) {
        List<ProductSalePropertyKey> salePropertyKeyList=salePropertyKeyService.querySalePropertyByProductId(spuId);
        return RetVal.ok(salePropertyKeyList);
    }
    //3.保存商品SKU http://127.0.0.1/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }

    //4.商品SKU分页列表查询 http://127.0.0.1/product/querySkuInfoByPage/1/10
    @GetMapping("querySkuInfoByPage/{currentPageNum}/{pageSize}")
    public RetVal querySkuInfoByPage(@PathVariable Long currentPageNum,
                                     @PathVariable Long pageSize) {
        IPage<SkuInfo> page = new Page<>(currentPageNum, pageSize);
        skuInfoService.page(page,null);
        return RetVal.ok(page);
    }

    //5.商品SKU下架 http://127.0.0.1/product/offSale/24
    @GetMapping("offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        //0代表下架
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);
        //TODO 后面涉及到搜索环节的时候再编写
        //searchFeignClient.offSale(skuId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.OFF_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }
    //6.商品SKU下架 http://127.0.0.1/product/onSale/24
    @GetMapping("onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        //0代表下架
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);
        //TODO 后面涉及到搜索环节的时候再编写
        //searchFeignClient.onSale(skuId);
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.ON_SALE_ROUTING_KEY,skuId);

        return RetVal.ok();
    }





}

