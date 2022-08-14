package com.atguigu.service.impl;

import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.mapper.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {
    @Autowired
    private SkuPlatformPropertyValueService skuPlatformPropertyValueService;
    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;
    @Autowired
    private SkuImageService skuImageService;

    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //a.保存SKU的基本信息
        baseMapper.insert(skuInfo);
        Long skuId = skuInfo.getId();
        Long productId = skuInfo.getProductId();
        //b.保存SKU的平台属性
        List<SkuPlatformPropertyValue> skuPlatformPropertyValueList = skuInfo.getSkuPlatformPropertyValueList();
        if(!CollectionUtils.isEmpty(skuPlatformPropertyValueList)) {
            for (SkuPlatformPropertyValue skuPlatformProperty : skuPlatformPropertyValueList) {
                skuPlatformProperty.setSkuId(skuId);
            }
            skuPlatformPropertyValueService.saveBatch(skuPlatformPropertyValueList);
        }
        //c.保存SKU的销售属性
        List<SkuSalePropertyValue> skuSalePropertyValueList = skuInfo.getSkuSalePropertyValueList();
        if(!CollectionUtils.isEmpty(skuSalePropertyValueList)) {
            for (SkuSalePropertyValue skuSalePropertyValue : skuSalePropertyValueList) {
                //该销售属性属于哪个sku
                skuSalePropertyValue.setSkuId(skuId);
                //该销售属性属于哪个spu
                skuSalePropertyValue.setProductId(productId);
            }
            skuSalePropertyValueService.saveBatch(skuSalePropertyValueList);
        }
        //d.保存SKU的图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
            }
            skuImageService.saveBatch(skuImageList);
        }
    }
}
