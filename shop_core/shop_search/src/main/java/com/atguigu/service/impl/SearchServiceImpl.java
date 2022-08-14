package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.client.ProductFeignClient;
import com.atguigu.dao.ProductMapper;
import com.atguigu.entity.BaseBrand;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.search.*;
import com.atguigu.service.SearchService;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Override
    public void onSale(Long skuId) {
        Product product = new Product();
        //a.商品基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if(skuInfo!=null){
            product.setId(skuInfo.getId());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            //b.品牌信息
            Long brandId = skuInfo.getBrandId();
            BaseBrand brand = productFeignClient.getBrandById(brandId);
            if(brand!=null){
                product.setBrandId(brand.getId());
                product.setBrandName(brand.getBrandName());
                product.setBrandLogoUrl(brand.getBrandLogoUrl());
            }
            //c.商品的分类信息
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            if(categoryView!=null){
                product.setCategory1Id(categoryView.getCategory1Id());
                product.setCategory1Name(categoryView.getCategory1Name());
                product.setCategory2Id(categoryView.getCategory2Id());
                product.setCategory2Name(categoryView.getCategory2Name());
                product.setCategory3Id(categoryView.getCategory3Id());
                product.setCategory3Name(categoryView.getCategory3Name());
            }
            //d.根据skuId查询商品的平台属性
            List<PlatformPropertyKey> platformPropertyList = productFeignClient.getPlatformPropertyBySkuId(skuId);
            if(!CollectionUtils.isEmpty(platformPropertyList)){
                List<SearchPlatformProperty> searchPlatformList = platformPropertyList.stream().map(platformPropertyKey -> {
                    SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                    //平台属性id
                    searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                    //平台属性值
                    String propertyValue = platformPropertyKey.getPropertyValueList().get(0).getPropertyValue();
                    searchPlatformProperty.setPropertyValue(propertyValue);
                    //平台属性key
                    searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());
                    return searchPlatformProperty;
                }).collect(Collectors.toList());
                product.setPlatformProperty(searchPlatformList);
            }
        }
        //e.存储到ES中
        productMapper.save(product);
    }

    @Override
    public void offSale(Long skuId) {
        //从es中删除
        productMapper.deleteById(skuId);

    }
    //将当前方法抛出的异常，包装成RuntimeException，骗过编译器，使得调用点可以不用显示处理异常信息
    @SneakyThrows
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //1.生成商品搜索DSL语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //2.实现对DSL语句的调用
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //3.把查询出来的结果封装解析出来
        SearchResponseVo searchResponseVo=parseSearchResult(searchResponse);
        //4.还需要设置其他参数信息
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //5.设置总页数
        boolean flag=searchResponseVo.getTotal()%searchParam.getPageSize()==0;
        long totalPages=0;
        if(flag){
            totalPages=searchResponseVo.getTotal()/searchParam.getPageSize();
        }else{
            totalPages=searchResponseVo.getTotal()/searchParam.getPageSize()+1;
        }
        searchResponseVo.setTotalPages(totalPages);
        return searchResponseVo;
    }

    //3.把查询出来的结果封装解析出来
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo=new SearchResponseVo();
        //1.拿到商品的基本信息
        SearchHits firstHits = searchResponse.getHits();
        //总记录数
        long totalHits = firstHits.getTotalHits();
        searchResponseVo.setTotal(totalHits);
        SearchHit[] secondHits = firstHits.getHits();
        if(secondHits!=null&&secondHits.length>0){
            for (SearchHit secondHit : secondHits) {
                //商品的基本信息
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                //拿到高亮替换source里面的productName
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if(highlightField!=null){
                    Text fragment=highlightField.getFragments()[0];
                    product.setProductName(fragment.toString());
                }
                searchResponseVo.getProductList().add(product);
            }
        }
        //2.拿到商品的品牌信息
        ParsedLongTerms brandIdAgg=searchResponse.getAggregations().get("brandIdAgg");
        List<SearchBrandVo> brandVoList = brandIdAgg.getBuckets().stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //品牌id
            String brandId = bucket.getKeyAsString();
            searchBrandVo.setBrandId(Long.parseLong(brandId));
            //品牌名称
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //图片地址
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String brandLogoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(brandLogoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(brandVoList);
        //3.拿到商品的平台属性聚合信息
        ParsedNested platformPropertyAgg=searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg=platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyList = propertyKeyIdAgg.getBuckets().stream().map(bucket -> {
            SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
            Number properteyId = bucket.getKeyAsNumber();
            //平台属性Id
            searchPlatformPropertyVo.setPropertyKeyId(properteyId.longValue());
            //属性名称
            ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
            String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
            searchPlatformPropertyVo.setPropertyKey(propertyKey);
            //当前属性值的集合
            ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
//            List<String> propertyValueList = propertyValueAgg.getBuckets().stream().map(innerBucket -> {
//                return innerBucket.getKeyAsString();
//            }).collect(Collectors.toList());
            List<String> propertyValueList =propertyValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
            return searchPlatformPropertyVo;
        }).collect(Collectors.toList());

        searchResponseVo.setPlatformPropertyList(platformPropertyList);
        return searchResponseVo;
    }

    //1.生成商品搜索DSL语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //1.构造一个query
        SearchSourceBuilder esBuilder = new SearchSourceBuilder();
        //2.构造一个bool
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //3.构造一个filter
        if(!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //构造一级分类过滤器
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        if(!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //构造二级分类过滤器
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        if(!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //构造三级分类过滤器
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //4.构造一个品牌过滤器brandName=1:苹果
        String brandName = searchParam.getBrandName();
        if(!StringUtils.isEmpty(brandName)){
            String[] brandParam = brandName.split(":");
            if(brandParam.length==2){
                firstBool.filter(QueryBuilders.termQuery("brandId", brandParam[0]));
            }
        }
        //5.构造关键字查询 keyword=手机
        String keyword = searchParam.getKeyword();
        if(!StringUtils.isEmpty(keyword)) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("productName", keyword).operator(Operator.OR);
            firstBool.must(matchQuery);
        }
        //6.构造平台属性过滤器 &props=4:骁龙888:CPU型号&&props=5:6.0～6.24英寸:屏幕尺寸
        String[] props = searchParam.getProps();
        if(props!=null&&props.length>0){
            for (String prop : props) {
                //props=4:骁龙888:CPU型号
                String[] platformParams = prop.split(":");
                if(platformParams.length==3){
                    BoolQueryBuilder secondBool = QueryBuilders.boolQuery();
                    BoolQueryBuilder childBool = QueryBuilders.boolQuery();
                    childBool.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platformParams[0]));
                    childBool.must(QueryBuilders.termQuery("platformProperty.propertyValue", platformParams[1]));
                    secondBool.must(QueryBuilders.nestedQuery("platformProperty",childBool, ScoreMode.None));
                    firstBool.filter(secondBool);
                }

            }
        }
        //把firstBool放到query里面去
        esBuilder.query(firstBool);
        //7.构造分页
        int from=(searchParam.getPageNo()-1)*searchParam.getPageSize();
        esBuilder.from(from);
        esBuilder.size(searchParam.getPageSize());
        /**
         * 8.构造排序 &order=2:asc /  &order=1:desc
         * 1--综合排序  hotScore
         * 2--价格排序  price
         */
        String pageOrder = searchParam.getOrder();
        if(!StringUtils.isEmpty(pageOrder)) {
            String[] orderParam = pageOrder.split(":");
            if(orderParam.length==2){
                String fileName="";
                switch (orderParam[0]){
                    case "1":
                        fileName="hotScore";
                        break;
                    case "2":
                        fileName="price";
                        break;
                }
                //String name, SortOrder order
                esBuilder.sort(fileName,"asc".equals(orderParam[1])? SortOrder.ASC:SortOrder.DESC);
            }
        }else {
            //如果没有选择排序方式 选择默认排序
            esBuilder.sort("hotScore",SortOrder.DESC);
        }
        //9.构造高亮
        HighlightBuilder highlightBuilder=new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        esBuilder.highlighter(highlightBuilder);
        //10.构造品牌聚合
        TermsAggregationBuilder brandIdAggBuilder=AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        esBuilder.aggregation(brandIdAggBuilder);
        //11.构造平台属性聚合
        esBuilder.aggregation( AggregationBuilders.nested("platformPropertyAgg","platformProperty")
                .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                        .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                        .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));
        //12.查询哪个index和type
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(esBuilder);
        System.out.println("拼接好的DSL语句"+esBuilder.toString());
        return searchRequest;

    }
}
