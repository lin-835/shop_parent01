package com.atguigu.controller;

import com.atguigu.client.SecKillFeignClient;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
public class WebSecKillController {
    @Autowired
    private SecKillFeignClient secKillFeignClient;

    //1.跳转到秒杀首页列表
    @RequestMapping("/seckill-index.html")
    public String seckillIndex(Model model){
        List<SeckillProduct> seckillProductList = secKillFeignClient.queryAllSecKillProduct();
        model.addAttribute("list",seckillProductList);
        return "seckill/index";
    }

    //2.秒杀商品详情
    @RequestMapping("/seckill-detail/{skuId}.html")
    public String seckillDetail(@PathVariable Long skuId, Model model){
        SeckillProduct seckillProduct = secKillFeignClient.querySecKillProductById(skuId);
        model.addAttribute("item",seckillProduct);
        return "seckill/detail";
    }

    //3.获取抢购码成功之后的页面
    @RequestMapping("/seckill-queue.html")
    public String seckillQueue(Long skuId,String seckillCode,Model model){
        model.addAttribute("skuId",skuId);
        model.addAttribute("seckillCode",seckillCode);
        return "seckill/queue";
    }

    //4.秒杀确认订单
    @RequestMapping("/seckill-confirm.html")
    public String seckillConfirm(Model model){
        //从秒杀系统中获取秒杀订单确认信息
        RetVal retVal = secKillFeignClient.seckillConfirm();
        if(retVal.isOk()){
            Map<String, Object> retMap=(Map<String, Object>)retVal.getData();
            model.addAllAttributes(retMap);
        }else{
            model.addAttribute("message",retVal.getMessage());
        }
        return "seckill/confirm";
    }





}
