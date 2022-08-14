package com.atguigu.service;

import com.atguigu.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-03
 */
public interface UserInfoService extends IService<UserInfo> {

    UserInfo queryUserFromDb(UserInfo uiUserInfo);
}
