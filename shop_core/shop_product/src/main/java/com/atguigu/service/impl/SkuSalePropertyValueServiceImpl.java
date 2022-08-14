package com.atguigu.service.impl;

import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuSalePropertyValueMapper;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * sku销售属性值 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@Service
public class SkuSalePropertyValueServiceImpl extends ServiceImpl<SkuSalePropertyValueMapper, SkuSalePropertyValue> implements SkuSalePropertyValueService {

    @Override
    public List<Map> getSalePropertyAndSkuIdMapping(Long productId) {
        return baseMapper.getSalePropertyAndSkuIdMapping(productId);
    }
}
