package com.atguigu.service;

import com.atguigu.entity.BaseBrand;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 品牌表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-22
 */
public interface BaseBrandService extends IService<BaseBrand> {

    void setNum();
}
