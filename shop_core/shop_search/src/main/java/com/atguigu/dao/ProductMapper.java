package com.atguigu.dao;

import com.atguigu.search.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductMapper extends ElasticsearchRepository<Product,Long> {
}
