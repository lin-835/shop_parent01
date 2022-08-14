package com.atguigu.service.impl;

import com.atguigu.client.UserFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.*;
import com.atguigu.mapper.SeckillProductMapper;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-12
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserFeignClient userFeignClient;


    public SeckillProduct getSeckillById(@PathVariable Long skuId) {
        return (SeckillProduct)redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
    }

    @Override
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo) {
        Long skuId = userSeckillSkuInfo.getSkuId();
        String userId = userSeckillSkuInfo.getUserId();
        String state=(String)redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX+skuId);
        //商品已经售罄
        if(RedisConst.CAN_NOT_SECKILL.equals(state)){
            return;
        }
        //判断用户是否下过预购单 prepare:seckill:userId:skuId:3:24
        boolean flag=redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID+":"+userId+":"+skuId,skuId,RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        if(!flag){
            return;
        }
        //校验库存是否足够 如果有库存减库存
        String redisStockSkuId=(String)redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+skuId).rightPop();
        if(StringUtils.isEmpty(redisStockSkuId)){
            //没有库存 通知其他redis节点修改秒杀状态位
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_NOT_SECKILL);
            return;
        }
        //生成一个临时订单到redis当中  prepare:seckill:userId:order
        PrepareSeckillOrder prepareSeckillOrder = new PrepareSeckillOrder();
        prepareSeckillOrder.setUserId(userId);
        //商品基本信息
        SeckillProduct seckillProduct = getSeckillById(skuId);
        prepareSeckillOrder.setSeckillProduct(seckillProduct);
        prepareSeckillOrder.setBuyNum(1);
        //生成一个订单码
        prepareSeckillOrder.setPrepareOrderCode(MD5.encrypt(userId+skuId));
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId,prepareSeckillOrder);
        //更新库存信息
        updateSecKillStockCount(skuId);

    }

    @Override
    public RetVal hasQualified(String userId, Long skuId) {
        //如果预下单里面有用户的信息 就代表具备抢购资格 prepare:seckill:userId:skuId:3:24
        boolean isExist=redisTemplate.hasKey(RedisConst.PREPARE_SECKILL_USERID_SKUID+":"+userId+":"+skuId);
        if(isExist){
            //拿出用户的预购单信息
            PrepareSeckillOrder prepareSeckillOrder=(PrepareSeckillOrder)redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
            if(prepareSeckillOrder!=null){
                return RetVal.build(prepareSeckillOrder, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
            }
        }
        //如果用户已经购买过该商品
        Integer orderId = (Integer)redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).get(userId);
        if(orderId!=null){
            /**
             * 如果订单id不为空 代表该用户购买过该商品 此时页面不能显示为排队中
             * 显示SECKILL_ORDER_SUCCESS 代表抢购成功---页面显示为抢购成功
             */
            return RetVal.build(null, RetValCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //如果预下单里面没有用户的信息
        return RetVal.build(null, RetValCodeEnum.SECKILL_RUN);
    }



    private void updateSecKillStockCount(Long skuId) {
        //剩余库存量
        Long leftStock = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //更新库存的频次 自定义一个规则
        if(leftStock%2==0){
            SeckillProduct seckillProduct = getSeckillById(skuId);
            //锁定库存量=商品总数量-剩余库存量
            int lockStock=seckillProduct.getNum()-Integer.parseInt(leftStock+"");
            seckillProduct.setStockCount(lockStock);
            //更新redis的商品锁定库存量目的是为了给用户看进度
            redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId.toString(),seckillProduct);
            //更新数据库为了持久化库存信息 防止丢失
            baseMapper.updateById(seckillProduct);
        }
    }

    @Override
    public RetVal seckillConfirm(String userId) {
        //用户收货地址信息 shop-user
        List<UserAddress> userAddressList = userFeignClient.getUserAddressByUserId(userId);
        //获取用户预购清单
        PrepareSeckillOrder prepareSeckillOrder=(PrepareSeckillOrder)redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if(prepareSeckillOrder==null){
            return RetVal.fail().message("非法请求");
        }
        //把预购单里面的信息转换为订单详情
        SeckillProduct seckillProduct = prepareSeckillOrder.getSeckillProduct();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillProduct.getSkuId());
        orderDetail.setSkuName(seckillProduct.getSkuName());
        orderDetail.setImgUrl(seckillProduct.getSkuDefaultImg());
        orderDetail.setSkuNum(prepareSeckillOrder.getBuyNum()+"");

        //订单的价格 不拿实时价格
        orderDetail.setOrderPrice(seckillProduct.getCostPrice());
        List<OrderDetail> orderDetailList = new ArrayList<>();
        orderDetailList.add(orderDetail);

        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList",userAddressList);
        retMap.put("orderDetailList",orderDetailList);
        retMap.put("totalMoney",seckillProduct.getCostPrice());
        return RetVal.ok(retMap);
    }
}
