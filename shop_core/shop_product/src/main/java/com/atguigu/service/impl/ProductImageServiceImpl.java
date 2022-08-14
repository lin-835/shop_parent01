package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.mapper.ProductImageMapper;
import com.atguigu.service.ProductImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品图片表 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {

}
