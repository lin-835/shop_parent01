package com.atguigu.controller;


import com.atguigu.entity.SkuInfo;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/init")
public class BloomController {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private RBloomFilter<Long> skuBloomFilter;

    //TODO 加入定时任务 每隔一段时间进行同步
    // 手动初始化 每次我们可以删除布隆过滤器里面的所有内容
    @GetMapping("/sku/bloom")
    public String skuBloom() {
        skuBloomFilter.delete();
        skuBloomFilter.tryInit(10000,0.001);

        //加载数据库中所有的数据id
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        List<SkuInfo> skuInfoList = skuInfoService.list(wrapper);
        for (SkuInfo skuInfo : skuInfoList) {
            Long skuId = skuInfo.getId();
            //以HASH散列放到布隆过滤器里面
            skuBloomFilter.add(skuId);
        }
        return "success";
    }
}

