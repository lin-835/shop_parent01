package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.mapper.BaseCategoryViewMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-07-25
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {

    @Override
    public List<JSONObject> getIndexCategory() {
        //b.查询所有的分类信息
        List<BaseCategoryView> allCategoryViewList = baseMapper.selectList(null);
        //c.找到所有的一级分类
        Map<Long, List<BaseCategoryView>> category1Map = allCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //所有的一级分类json数据
        List<JSONObject> allCategoryJson=new ArrayList<>();
        Integer index=0;
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {
            Long category1Id = category1Entry.getKey();
            List<BaseCategoryView> category1List = category1Entry.getValue();
            //构造一个json格式数据(一级分类)
            JSONObject category1Json = new JSONObject();
            category1Json.put("index",++index);
            category1Json.put("categoryId",category1Id);
            category1Json.put("categoryName",category1List.get(0).getCategory1Name());
            //d.找到所有的二级分类
            List<JSONObject> category1Children=new ArrayList<>();
            Map<Long, List<BaseCategoryView>> category2Map = category1List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            category2Map.entrySet().forEach(category2Entry->{
                Long category2Id = category2Entry.getKey();
                List<BaseCategoryView> category2List = category2Entry.getValue();
                //构造一个json格式数据(二级分类)
                JSONObject category2Json = new JSONObject();
                category2Json.put("categoryId",category2Id);
                category2Json.put("categoryName",category2List.get(0).getCategory2Name());
                //e.找到所有的三级分类
                List<JSONObject> category2Children=new ArrayList<>();
                Map<Long, List<BaseCategoryView>> category3Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                category3Map.entrySet().forEach(category3Entry->{
                    Long category3Id = category3Entry.getKey();
                    List<BaseCategoryView> category3List = category3Entry.getValue();
                    //构造一个json格式数据(三级分类)
                    JSONObject category3Json = new JSONObject();
                    category3Json.put("categoryId",category3Id);
                    category3Json.put("categoryName",category3List.get(0).getCategory3Name());
                    category2Children.add(category3Json);
                });
                category2Json.put("categoryChild",category2Children);
                //一级分类的子节点
                category1Children.add(category2Json);
            });
            category1Json.put("categoryChild",category1Children);
            allCategoryJson.add(category1Json);
        }
        return allCategoryJson;
    }
}
