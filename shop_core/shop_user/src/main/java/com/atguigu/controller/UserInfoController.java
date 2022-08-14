package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-03
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private RedisTemplate redisTemplate;

    //1.登录的认证逻辑
    @PostMapping("login")
    public RetVal login(@RequestBody UserInfo uiUserInfo, HttpServletRequest request) {
        //a.根据账号密码查询数据库看是否有记录
        UserInfo dbUserInfo = userInfoService.queryUserFromDb(uiUserInfo);
        if (dbUserInfo != null) {
            Map<String, Object> retMap = new HashMap<>();
            //b.生成一个token返回给前端(cookie中)
            String token = UUID.randomUUID().toString();
            retMap.put("token", token);
            //返回用户昵称给前端
            String nickName = dbUserInfo.getNickName();
            retMap.put("nickName", nickName);
            //c.将用户信息存储到redis中 作用后面再说  user:login:xxxx
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userInfoJson = new JSONObject();
            userInfoJson.put("userId", dbUserInfo.getId());
            userInfoJson.put("loginIp", IpUtil.getIpAddress(request));
            redisTemplate.opsForValue().set(userKey, userInfoJson, RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            return RetVal.ok(retMap);
        } else {
            return RetVal.fail().message("登录失败");
        }
    }

    //2.登出功能
    @GetMapping("logout")
    public RetVal logout(HttpServletRequest request) {
        String tokenValue="";
        //此时尝试从前端页面cookie中拿token
//        Cookie[] cookies = request.getCookies();
//        if(cookies!=null){
//            for (Cookie cookie : cookies) {
//                if("token".equals(cookie.getName())){
//                    tokenValue = cookie.getValue();
//                }
//            }
//        }
        tokenValue = request.getHeader("token");
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + tokenValue;;
        redisTemplate.delete(userKey);
        return RetVal.ok();
    }

}

