package com.atguigu.service;

import com.atguigu.entity.CartInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-05
 */
public interface CartInfoService extends IService<CartInfo> {

    void addToCart(String userTempId, Long skuId, Integer skuNum);

    List<CartInfo> getCartList(HttpServletRequest request);

    void checkCart(String oneOfUserId, Long skuId, Integer isChecked);

    void deleteCart(String oneOfUserId, Long skuId);

    List<CartInfo> getSelectedCartInfo(String userId);
}
