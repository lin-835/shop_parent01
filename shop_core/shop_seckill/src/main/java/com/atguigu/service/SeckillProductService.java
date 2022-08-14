package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-12
 */
public interface SeckillProductService extends IService<SeckillProduct> {

    SeckillProduct getSeckillById(Long skuId);

    void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo);

    RetVal hasQualified(String userId, Long skuId);

    RetVal seckillConfirm(String userId);
}
