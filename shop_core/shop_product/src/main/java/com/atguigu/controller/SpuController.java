package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-24
 */
@RestController
@RequestMapping("/product")
public class SpuController {
    @Autowired
    private ProductSpuService spuService;
    @Autowired
    private BaseSalePropertyService salePropertyService;

    //1.根据商品分类id查询商品SPU列表 http://127.0.0.1/product/queryProductSpuByPage/1/10/62
    @GetMapping("queryProductSpuByPage/{currentPageNum}/{pageSize}/{category3Id}")
    public RetVal queryBrandByPage(@PathVariable Long currentPageNum,
                                   @PathVariable Long pageSize,
                                   @PathVariable Long category3Id) {
        IPage<ProductSpu> page = new Page<>(currentPageNum, pageSize);
        QueryWrapper<ProductSpu> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id",category3Id);
        spuService.page(page,wrapper);
        return RetVal.ok(page);
    }
    //2.查询所有的销售属性 http://127.0.0.1/product/queryAllSaleProperty
    @GetMapping("queryAllSaleProperty")
    public RetVal queryAllSaleProperty() {
        List<BaseSaleProperty> baseSaleProperties = salePropertyService.list(null);
        return RetVal.ok(baseSaleProperties);
    }

    //3.添加SPU信息实战 http://127.0.0.1/product/saveProductSpu
    @PostMapping("saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu) {
        spuService.saveProductSpu(productSpu);
        return RetVal.ok();
    }




}

