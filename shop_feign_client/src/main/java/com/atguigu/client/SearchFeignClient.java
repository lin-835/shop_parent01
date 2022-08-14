package com.atguigu.client;

import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

//该接口是由哪个微服务去实现
@FeignClient(value = "shop-search")
public interface SearchFeignClient {
    //2.商品的上架
    @GetMapping("/search/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId);

    //3.商品的下架
    @GetMapping("/search/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId);

    //4.商品的搜索
    @PostMapping("/search/searchProduct")
    public RetVal searchProduct(SearchParam searchParam);
}
