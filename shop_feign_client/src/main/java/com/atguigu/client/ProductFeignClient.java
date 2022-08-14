package com.atguigu.client;

import com.atguigu.entity.*;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-product")
public interface ProductFeignClient {
    //a.根据skuId查询商品的基本信息
    @GetMapping("/sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);
    //b.根据三级分类id查询商品的分类
    @GetMapping("/sku/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    //c.根据skuId查询商品的实时价格
    @GetMapping("/sku/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);
    //d.销售属性id的组合与skuId的对应关系
    @GetMapping("/sku/getSalePropertyAndSkuIdMapping/{productId}")
    public Map<Object, Object> getSalePropertyAndSkuIdMapping(@PathVariable Long productId);
    //e.获取所有的销售属性(spu全份)和sku的销售属性(一份)
    @GetMapping("/sku/getSpuSalePropertyAndSelected/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId, @PathVariable Long skuId);

    //f.首页商品分类数据信息的显示
    @GetMapping("/product/getIndexCategory")
    public RetVal getIndexCategory();

    //g.根据id查询品牌信息
    @GetMapping("/product/brand/getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId);

    //h.根据skuId查询商品的平台属性
    @GetMapping("/product/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId);
}
