package com.atguigu.controller;

import com.atguigu.client.ProductFeignClient;
import com.atguigu.client.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebIndexController {
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private  SearchFeignClient searchFeignClient;

    @RequestMapping({"/","index.html"})
    public String index(Model model){
        RetVal retVal = productFeignClient.getIndexCategory();
        model.addAttribute("list",retVal.getData());
        return "index/index";
    }

    //2.商品搜索功能
    @GetMapping("search.html")
    public String searchProduct(SearchParam searchParam,Model model){
        //通过远程PRC调用search微服务实现搜索功能 搜索得到结果之后给页面
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        //不要写成 model.addAttribute() 需要添加多个属性
        model.addAllAttributes(retVal.getData());

        //1.搜索路径上参数的回显
        String urlParam=pageUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);
        //2.页面回显品牌信息
        String brandNameParam=pageBrandParam(searchParam.getBrandName());
        model.addAttribute("brandNameParam",brandNameParam);
        //3.页面回显平台属性信息
        List<Map<String, String>> propsParamList=pagePlatformParam(searchParam.getProps());
        model.addAttribute("propsParamList",propsParamList);
        //4.页面排序信息的回显
        Map<String, String> orderMap=pageSortParam(searchParam.getOrder());
        model.addAttribute("orderMap",orderMap);
        return "search/index";
    }

    //&order=2:desc
    private Map<String, String> pageSortParam(String order) {
        Map<String, String> orderMap=new HashMap<>();
        if(!StringUtils.isEmpty(order)){
            String[] orderSplit = order.split(":");
            if(orderSplit.length==2){
                orderMap.put("type",orderSplit[0]);
                orderMap.put("sort",orderSplit[1]);
            }
        }else{
            //给一个默认排序方式
            orderMap.put("type","1");
            orderMap.put("sort","desc");
        }
        return orderMap;
    }

    //&props=5:5.0英寸以下:屏幕尺寸&props=4:骁龙888:CPU型号
    private List pagePlatformParam(String[] props) {
        List<Map<String, String>> list=new ArrayList<>();
        if(props!=null&&props.length>0){
            for (String prop : props) {
                //props=4:骁龙888:CPU型号
                String[] propSplit = prop.split(":");
                if(propSplit.length==3){
                    Map<String, String> propMap = new HashMap<>();
                    propMap.put("propertyKeyId",propSplit[0]);
                    propMap.put("propertyValue",propSplit[1]);
                    propMap.put("propertyKey",propSplit[2]);
                    list.add(propMap);
                }

            }
        }
        return list;
    }

    //&brandName=3:三星
    private String pageBrandParam(String brandName) {
        if(!StringUtils.isEmpty(brandName)){
            String[] brandSplit = brandName.split(":");
            if(brandSplit.length==2){
                return "品牌:"+brandSplit[1];
            }
        }
        return null;
    }

    private String pageUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断是否有关键字 ?keyword=苹果手机
        if(!StringUtils.isEmpty(searchParam.getKeyword())){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断是否有品牌 &brandName=3:三星
        if(!StringUtils.isEmpty(searchParam.getBrandName())){
            //保证已经有了一个keyword 才能往下接着拼接
            if(urlParam.length()>0){
                urlParam.append("&brandName=").append(searchParam.getBrandName());
            }

        }
        //判断是否有平台属性 &props=5:5.0英寸以下:屏幕尺寸&props=4:骁龙888:CPU型号
        if(!StringUtils.isEmpty(searchParam.getProps())){
            //保证已经有了一个keyword 才能往下接着拼接
            if(urlParam.length()>0){
                for (String prop : searchParam.getProps()) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "search.html?"+urlParam.toString();
    }

}
