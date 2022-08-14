package com.atguigu.controller;


import com.atguigu.entity.BaseBrand;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseBrandService;
import com.atguigu.utils.MinioUploader;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 品牌表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-22
 */
@RestController
@RequestMapping("/product/brand")
public class BrandController {
    @Autowired
    private MinioUploader minioUploader;
    @Autowired
    private BaseBrandService brandService;
    //1.分页查询品牌列表 http://192.168.15.218/product/brand/queryBrandByPage/1/10
    @GetMapping("queryBrandByPage/{currentPageNum}/{pageSize}")
    public RetVal queryBrandByPage(@PathVariable Long currentPageNum, @PathVariable Long pageSize) {
        IPage<BaseBrand> page = new Page<>(currentPageNum, pageSize);
        brandService.page(page, null);
        return RetVal.ok(page);
    }

    //http://127.0.0.1/product/brand
    //2.添加品牌
    @PostMapping
    public RetVal saveBrand(@RequestBody BaseBrand brand) {
        brandService.save(brand);
        return RetVal.ok();
    }

    //http://127.0.0.1/product/brand/4
    //3.根据id查询品牌信息
    @GetMapping("{brandId}")
    public RetVal saveBrand(@PathVariable Long brandId) {
        BaseBrand brand = brandService.getById(brandId);
        return RetVal.ok(brand);
    }

    //4.更新品牌信息
    @PutMapping
    public RetVal updateBrand(@RequestBody BaseBrand brand) {
        brandService.updateById(brand);
        return RetVal.ok();
    }

    //5.删除品牌信息
    @DeleteMapping("{brandId}")
    public RetVal remove(@PathVariable Long brandId) {
        brandService.removeById(brandId);
        return RetVal.ok();
    }

    //6.查询所有的品牌
    //@ShopCache(prefix="allBrand",enableBloom = false)
    @GetMapping("getAllBrand")
    public RetVal getAllBrand() {
        List<BaseBrand> brandList = brandService.list(null);
        return RetVal.ok(brandList);
    }
    //7.上传文件的接口
    @PostMapping("fileUpload")
    public RetVal fileUpload(MultipartFile file) throws Exception {
        String retUrl = minioUploader.uploadFile(file);
        return RetVal.ok(retUrl);
    }

    //8.根据id查询品牌信息
    @GetMapping("getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId) {
        BaseBrand brand = brandService.getById(brandId);
        return brand;
    }

}

