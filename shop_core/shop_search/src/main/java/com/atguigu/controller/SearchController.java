package com.atguigu.controller;

import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.SearchService;
import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    private ElasticsearchRestTemplate esRestTemplate;
    @Autowired
    private SearchService searchService;

    //1.创建索引与映射
    @GetMapping("createIndex")
    public RetVal createIndex(){
        esRestTemplate.createIndex(Product.class);
        esRestTemplate.putMapping(Product.class);
       return RetVal.ok();
    }

    //2.商品的上架
    @GetMapping("onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId){
        searchService.onSale(skuId);
        return RetVal.ok();
    }

    //3.商品的下架
    @GetMapping("offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){
        searchService.offSale(skuId);
        return RetVal.ok();
    }
    //4.商品的搜索
    @PostMapping("searchProduct")
    public RetVal searchProduct(@RequestBody SearchParam searchParam){
        SearchResponseVo searchResponseVo=searchService.searchProduct(searchParam);
        return RetVal.ok(searchResponseVo);
    }


}
