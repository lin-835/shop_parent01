package com.atguigu.service;

import com.atguigu.entity.SkuInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 库存单元表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
public interface SkuInfoService extends IService<SkuInfo> {

    void saveSkuInfo(SkuInfo skuInfo);
}
