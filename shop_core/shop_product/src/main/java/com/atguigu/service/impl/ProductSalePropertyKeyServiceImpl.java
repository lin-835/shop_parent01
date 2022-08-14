package com.atguigu.service.impl;

import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.mapper.ProductSalePropertyKeyMapper;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * spu销售属性 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@Service
public class ProductSalePropertyKeyServiceImpl extends ServiceImpl<ProductSalePropertyKeyMapper, ProductSalePropertyKey> implements ProductSalePropertyKeyService {

    @Override
    public List<ProductSalePropertyKey> querySalePropertyByProductId(Long spuId) {
        return baseMapper.querySalePropertyByProductId(spuId);
    }
}
