package com.atguigu.service;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;

import java.util.List;
import java.util.Map;

public interface SkuDetailService {
    SkuInfo getSkuInfo(Long skuId);

    Map<Object, Object> getSalePropertyAndSkuIdMapping(Long productId);

    List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId, Long skuId);
}
