package com.atguigu.service;

import com.atguigu.entity.SkuSalePropertyValue;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * sku销售属性值 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
public interface SkuSalePropertyValueService extends IService<SkuSalePropertyValue> {

    List<Map> getSalePropertyAndSkuIdMapping(Long productId);
}
