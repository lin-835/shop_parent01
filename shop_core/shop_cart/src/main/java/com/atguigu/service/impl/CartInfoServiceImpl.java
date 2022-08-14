package com.atguigu.service.impl;

import com.atguigu.client.ProductFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuInfo;
import com.atguigu.mapper.CartInfoMapper;
import com.atguigu.service.AsyncCartInfoService;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-05
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AsyncCartInfoService asyncCartInfoService;
    @Override
    public void addToCart(String oneOfUserId, Long skuId, Integer skuNum) {
        //a.查询数据库中是否存在该商品信息
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",oneOfUserId);
        wrapper.eq("sku_id",skuId);
        CartInfo existCartInfo = baseMapper.selectOne(wrapper);
        //b.如果存在
        if(existCartInfo!=null){
            //把原来的商品数量传递过来的数量
            existCartInfo.setSkuNum(existCartInfo.getSkuNum()+skuNum);
            //把最新商品的价格拿到
            existCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            //更新数据库
            baseMapper.updateById(existCartInfo);
        }else{
            //c.如果不存在 新增一条记录
            existCartInfo=new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            existCartInfo.setUserId(oneOfUserId);
            existCartInfo.setSkuId(skuId);
            existCartInfo.setCartPrice(skuInfo.getPrice());
            existCartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            existCartInfo.setSkuName(skuInfo.getSkuName());
            existCartInfo.setSkuNum(skuNum);
            //默认设置勾选该商品
            existCartInfo.setIsChecked(1);
            existCartInfo.setRealTimePrice(productFeignClient.getSkuPrice(skuId));
            baseMapper.insert(existCartInfo);
            //asyncCartInfoService.saveCartInfo(existCartInfo);
        }
        //d.往redis中怼一份
        String userCartKey=getUserCartKey(oneOfUserId);
        redisTemplate.boundHashOps(userCartKey).put(skuId.toString(),existCartInfo);


    }

    @Override
    public List<CartInfo> getCartList(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        String userTempId=AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList=new ArrayList<>();
        //1.未登录
        if(StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){
            cartInfoList = queryFromDbToRedis(userTempId);
        }
        //2.已登录
        if(!StringUtils.isEmpty(userId)){
            //查询未登录的购物车信息
            List<CartInfo> noLoginCartInfoList = queryFromDbToRedis(userTempId);
            if(!CollectionUtils.isEmpty(noLoginCartInfoList)){
                //合并已登录和未登录的购物项
                mergeCartInfoList(userId,userTempId);
                //合并之后删除临时用户的购物项
                cartInfoList=deleteNoLoginDataAndReload(userId,userTempId);
            }else{
                cartInfoList = queryFromDbToRedis(userId);
            }

        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String oneOfUserId, Long skuId, Integer isChecked) {
        //a.从redis中查询数据并修改(主要为了学习)
        String userCartKey = getUserCartKey(oneOfUserId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(userCartKey);
        //从hash里面根据skuId拿到购物车商品sku信息
        if(boundHashOps.hasKey(skuId.toString())){
            CartInfo redisCartInfo=(CartInfo)boundHashOps.get(skuId.toString());
            redisCartInfo.setIsChecked(isChecked);
            //更新到redis中
            boundHashOps.put(skuId.toString(),redisCartInfo);
            //设置过期时间
            setCartKeyExpire(userCartKey);
        }
        //b.修改数据库
        checkCartInfo(oneOfUserId, skuId, isChecked);
    }

    @Override
    public void deleteCart(String oneOfUserId, Long skuId) {
        //a.先删除redis里面的内容
        String userCartKey = getUserCartKey(oneOfUserId);
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(userCartKey);
        if(boundHashOps.hasKey(skuId.toString())){
            boundHashOps.delete(skuId.toString());
        }
        //b.删除数据库里面的内容
        deleteCartInfo(oneOfUserId, skuId);
    }

    @Override
    public List<CartInfo> getSelectedCartInfo(String userId) {
        String userCartKey = getUserCartKey(userId);
        List<CartInfo> redisCartInfoList = redisTemplate.opsForHash().values(userCartKey);
        List<CartInfo> selectedCartInfoList=new ArrayList<>();
        if(!CollectionUtils.isEmpty(redisCartInfoList)){
            for (CartInfo cartInfo : redisCartInfoList) {
                if(cartInfo.getIsChecked()==1){
                    selectedCartInfoList.add(cartInfo);
                }
            }
        }
        return selectedCartInfoList;
    }

    protected void deleteCartInfo(String oneOfUserId, Long skuId) {
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",oneOfUserId);
        wrapper.eq("sku_id",skuId);
        baseMapper.delete(wrapper);
    }

    protected void checkCartInfo(String oneOfUserId, Long skuId, Integer isChecked) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",oneOfUserId);
        wrapper.eq("sku_id",skuId);
        baseMapper.update(cartInfo,wrapper);
    }

    private List<CartInfo> deleteNoLoginDataAndReload(String userId, String userTempId) {
        //删除数据库里面的数据
        QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userTempId);
        baseMapper.delete(wrapper);
        //删除redis里面的缓存数据
        String userTempKey = getUserCartKey(userTempId);
        String userIdKey = getUserCartKey(userId);
        redisTemplate.delete(userIdKey);
        redisTemplate.delete(userTempKey);
        //重新加载数据到redis
        return  queryFromDbToRedis(userId);
    }

    private void mergeCartInfoList(String userId, String userTempId) {
        //未登录的购物项
        List<CartInfo> noLoginCartInfoList = queryFromDbToRedis(userTempId);
        //已登录的购物项
        List<CartInfo> loginCartInfoList = queryFromDbToRedis(userId);
//        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
//            for (CartInfo loginCartInfo : loginCartInfoList) {
//                //代表未登录和已登录都添加了该购物项
//                if(noLoginCartInfo.getSkuId().equals(loginCartInfo.getSkuId())){
//                    //把未登录和已登录数量相加
//                    loginCartInfo.setSkuNum(loginCartInfo.getSkuNum()+noLoginCartInfo.getSkuNum());
//                    //当未登录的时候该商品未勾选 合并之后需要勾选
//                    if(noLoginCartInfo.getIsChecked()==0){
//                        loginCartInfo.setIsChecked(1);
//                    }
//                    //更新数据库
//                    baseMapper.updateById(loginCartInfo);
//                }else{
//                    //如果已登录没有该购物项
//                    noLoginCartInfo.setUserId(userId);
//                    baseMapper.updateById(noLoginCartInfo);
//                }
//            }
//        }
        //把已登录的转换为map
        Map<Long, CartInfo> loginCartInfoMap = loginCartInfoList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            //代表未登录和已登录都添加了该购物项
                if(loginCartInfoMap.containsKey(noLoginCartInfo.getSkuId())){
                    CartInfo loginCartInfo = loginCartInfoMap.get(noLoginCartInfo.getSkuId());
                    //把未登录和已登录数量相加
                    loginCartInfo.setSkuNum(loginCartInfo.getSkuNum()+noLoginCartInfo.getSkuNum());
                    //当未登录的时候该商品未勾选 合并之后需要勾选
                    if(noLoginCartInfo.getIsChecked()==0){
                        loginCartInfo.setIsChecked(1);
                    }
                    //更新数据库
                    baseMapper.updateById(loginCartInfo);
                }else{
                    //如果已登录没有该购物项
                    noLoginCartInfo.setUserId(userId);
                    baseMapper.updateById(noLoginCartInfo);
                }
        }
    }

    private List<CartInfo> queryFromDbToRedis(String oneOfUserId) {
        String userCartKey=getUserCartKey(oneOfUserId);
        List<CartInfo> cartInfoList = redisTemplate.boundHashOps(userCartKey).values();
        //如果缓存里面没有信息
        if(CollectionUtils.isEmpty(cartInfoList)){
            QueryWrapper<CartInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id",oneOfUserId);
            cartInfoList = baseMapper.selectList(wrapper);
            //放入缓存
            Map<String, CartInfo> cartMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                //redisTemplate.boundHashOps(userCartKey).put(cartInfo.getSkuId().toString(),cartInfo);
                cartMap.put(cartInfo.getSkuId().toString(),cartInfo);
            }
            redisTemplate.boundHashOps(userCartKey).putAll(cartMap);
            //设置redis购物车过期时间
            setCartKeyExpire(userCartKey);
        }
        return cartInfoList;
    }

    private void setCartKeyExpire(String userCartKey) {
        redisTemplate.expire(userCartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    private String getUserCartKey(String oneOfUserId) {
        String userCartKey= RedisConst.USER_KEY_PREFIX+oneOfUserId+RedisConst.USER_CART_KEY_SUFFIX;
        return userCartKey;
    }
}
