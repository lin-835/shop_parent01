package com.atguigu.controller;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-05
 */
@RestController
@RequestMapping("/cart")
public class CartInfoController {
    @Autowired
    private CartInfoService cartInfoService;

    //1.加入购物车
    @GetMapping("addCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request){
        //在shop-cart中拿到用户id
        String oneOfUserId="";
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            oneOfUserId=AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId=userId;
        }
        cartInfoService.addToCart(oneOfUserId,skuId,skuNum);
        return RetVal.ok();
    }

    //2.购物车列表
    @GetMapping("getCartList")
    public RetVal getCartList(HttpServletRequest request){
        List<CartInfo> cartInfoList=cartInfoService.getCartList(request);
        return RetVal.ok(cartInfoList);
    }

    //3.购物车勾选 http://api.gmall.com/cart/checkCart/24/1
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public RetVal checkCart(@PathVariable Long skuId, @PathVariable Integer isChecked,HttpServletRequest request){
        //在shop-cart中拿到用户id
        String oneOfUserId="";
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            oneOfUserId=AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId=userId;
        }
        cartInfoService.checkCart(oneOfUserId,skuId,isChecked);
        return RetVal.ok();
    }
    //4.购物车删除 http://api.gmall.com/cart/deleteCart/24
    @DeleteMapping("deleteCart/{skuId}")
    public RetVal deleteCart(@PathVariable Long skuId,HttpServletRequest request){
        //在shop-cart中拿到用户id
        String oneOfUserId="";
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            oneOfUserId=AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId=userId;
        }
        cartInfoService.deleteCart(oneOfUserId,skuId);
        return RetVal.ok();
    }
    //5.查询勾选的购物车信息--送货清单  其他微服务调用
    @GetMapping("getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId){
        return cartInfoService.getSelectedCartInfo(userId);
    }


}

