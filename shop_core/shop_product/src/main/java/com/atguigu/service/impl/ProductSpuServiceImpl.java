package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.mapper.ProductSpuMapper;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {
    @Autowired
    private ProductImageService imageService;
    @Autowired
    private ProductSalePropertyKeyService salePropertyKeyService;
    @Autowired
    private ProductSalePropertyValueService salePropertyValueService;

    @Transactional
    @Override
    public void saveProductSpu(ProductSpu productSpu) {
        //a.保存spu基本信息
        baseMapper.insert(productSpu);
        Long spuId = productSpu.getId();
        //b.保存spu的图片信息
        List<ProductImage> productImageList = productSpu.getProductImageList();
        if(!CollectionUtils.isEmpty(productImageList)){
            for (ProductImage productImage : productImageList) {
                productImage.setProductId(spuId);
            }
            imageService.saveBatch(productImageList);
        }
        //c.保存spu销售属性key
        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        if(!CollectionUtils.isEmpty(salePropertyKeyList)){
            for (ProductSalePropertyKey salePropertyKey : salePropertyKeyList) {
                salePropertyKey.setProductId(spuId);
                //d.保存spu销售属性value
                List<ProductSalePropertyValue> salePropertyValueList = salePropertyKey.getSalePropertyValueList();
                if(!CollectionUtils.isEmpty(salePropertyValueList)){
                    for (ProductSalePropertyValue salePropertyValue : salePropertyValueList) {
                        salePropertyValue.setProductId(spuId);
                        salePropertyValue.setSalePropertyKeyName(salePropertyKey.getSalePropertyKeyName());
                    }
                    salePropertyValueService.saveBatch(salePropertyValueList);
                }
            }
            salePropertyKeyService.saveBatch(salePropertyKeyList);
        }
    }
}
