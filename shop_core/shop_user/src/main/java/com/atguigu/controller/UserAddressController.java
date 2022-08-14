package com.atguigu.controller;


import com.atguigu.entity.UserAddress;
import com.atguigu.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户地址表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-03
 */
@RestController
@RequestMapping("/user")
public class UserAddressController {
    @Autowired
    private UserAddressService userAddressService;

    //1.根据用户id查询用户的收货地址
    @GetMapping("getUserAddressByUserId/{userId}")
    public List<UserAddress> getUserAddressByUserId(@PathVariable String userId){
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        return userAddressService.list(wrapper);
    }

}

