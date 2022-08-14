package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-20
 */
@RestController
@RequestMapping("/product")
//@CrossOrigin
public class PlatformPropertyController {
    @Autowired
    private PlatformPropertyKeyService propertyKeyService;
    @Autowired
    private PlatformPropertyValueService propertyValueService;

    //1.根据分类id查询平台属性信息 http://127.0.0.1:8000/product/getPlatformPropertyByCategoryId/2/13/61
    @GetMapping("getPlatformPropertyByCategoryId/{category1Id}/{category2Id}/{category3Id}")
    public RetVal getPlatformPropertyByCategoryId(@PathVariable Long category1Id,
                                                  @PathVariable Long category2Id,
                                                  @PathVariable Long category3Id) {
        List<PlatformPropertyKey> platformPropertyKeyList = propertyKeyService.getPlatformPropertyByCategoryId(category1Id, category2Id, category3Id);
        return RetVal.ok(platformPropertyKeyList);
    }

    //2.根据平台属性keyId查询平台属性值 http://192.168.18.208/product/getPropertyValueByPropertyKeyId/4
    @GetMapping("getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId) {
        QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
        wrapper.eq("property_key_id", propertyKeyId);
        List<PlatformPropertyValue> propertyValueList = propertyValueService.list(wrapper);
        return RetVal.ok(propertyValueList);
    }

    //3.保存平台属性 http://127.0.0.1:8000/product/savePlatformProperty
    @PostMapping("savePlatformProperty")
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformPropertyKey) {
        boolean flag = propertyKeyService.savePlatformProperty(platformPropertyKey);
        if (flag) {
            return RetVal.ok();
        } else {
            return RetVal.fail();
        }
    }

    //4.根据skuId查询商品的平台属性
    @GetMapping("getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId) {
        return propertyKeyService.getPlatformPropertyBySkuId(skuId);
    }


}

