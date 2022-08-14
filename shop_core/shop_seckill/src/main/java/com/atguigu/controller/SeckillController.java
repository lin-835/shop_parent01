package com.atguigu.controller;


import com.atguigu.client.OrderFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MD5;
import com.atguigu.utils.DateUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zhangqiang
 * @since 2022-08-12
 */
@RestController
@RequestMapping("/seckill")
public class SeckillController {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;
    //1.秒杀商品列表显示
    @GetMapping("queryAllSecKillProduct")
    public List<SeckillProduct> queryAllSecKillProduct(){
        List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();
        return seckillProductList;
    }
    //2.单个秒杀商品详情
    @GetMapping("querySecKillProductById/{skuId}")
    public SeckillProduct querySecKillProductById(@PathVariable Long skuId){
        SeckillProduct seckillProduct = seckillProductService.getSeckillById(skuId);
        return seckillProduct;
    }



    //3.生成抢购码 http://api.gmall.com/seckill/generateSeckillCode/24
    @GetMapping("generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request){
        //1.判断用户是否登录
        String userId = AuthContextHolder.getUserId(request);
        if(!StringUtils.isEmpty(userId)){
            //2.从缓存中拿到秒杀商品信息
            SeckillProduct seckillProduct = seckillProductService.getSeckillById(skuId);
            Date nowDate = new Date();
            //3.判断当前时间是否在秒杀范围之内
            if(DateUtil.dateCompare(seckillProduct.getStartTime(),nowDate)&&
            DateUtil.dateCompare(nowDate,seckillProduct.getEndTime())){
                //4.利用md5对用户id进行加密 生成一个抢购码
                String secKillCode= MD5.encrypt(userId);
                return RetVal.ok(secKillCode);
            }
        }
        return RetVal.fail().message("获取抢购码失败，请先登录");
    }

    //4.秒杀预下单 http://api.gmall.com/seckill/prepareSeckill/33?seckillCode=eccbc87e4b5ce2fe28308fd9f2a7baf3
    @PostMapping("prepareSeckill/{skuId}")
    public RetVal prepareSeckill(@PathVariable Long skuId,String seckillCode, HttpServletRequest request) {
        //a.判断抢购码是否正确
        String userId = AuthContextHolder.getUserId(request);
        if(!MD5.encrypt(userId).equals(seckillCode)){
            //抢购码不合法 报异常
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }
        //b.判断秒杀商品是否可以进行秒杀 状态位为1可以秒杀
        String state=(String)redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX+skuId);
        if(StringUtils.isEmpty(state)){
            //秒杀状态位不存在
            return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
        }
        //c.如果可以秒杀就生成一个预下单
        if(RedisConst.CAN_SECKILL.equals(state)){
            UserSeckillSkuInfo userSeckillSkuInfo = new UserSeckillSkuInfo();
            userSeckillSkuInfo.setUserId(userId);
            userSeckillSkuInfo.setSkuId(skuId);
            rabbitTemplate.convertAndSend(MqConst.PREPARE_SECKILL_EXCHANGE,MqConst.PREPARE_SECKILL_ROUTE_KEY,userSeckillSkuInfo);
        }else{
            //秒杀商品已售罄
            return RetVal.build(null, RetValCodeEnum.SECKILL_FINISH);
        }
        return RetVal.ok();
    }

    //5.判断是否具备抢购资格 http://api.gmall.com/seckill/hasQualified/24
    @GetMapping("hasQualified/{skuId}")
    public RetVal hasQualified(@PathVariable Long skuId,HttpServletRequest request){
        String userId=AuthContextHolder.getUserId(request);
        return seckillProductService.hasQualified(userId,skuId);
    }
    //6.返回秒杀页面需要的数据
    @GetMapping("seckillConfirm")
    public RetVal seckillConfirm(HttpServletRequest request){
        String userId=AuthContextHolder.getUserId(request);
        return seckillProductService.seckillConfirm(userId);
    }
    //7.提交秒杀订单信息
    @PostMapping("submitSecKillOrder")
    public RetVal submitSecKillOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        String userId=AuthContextHolder.getUserId(request);
        //a.判断用户是否有预购单
        PrepareSeckillOrder prepareSeckillOrder=(PrepareSeckillOrder)redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if(prepareSeckillOrder==null){
            return RetVal.fail().message("非法请求");
        }
        //b.通过远程调用order微服务进行下单
        Long orderId = orderFeignClient.saveOrderAndDetail(orderInfo);
        if(orderId==null){
            return RetVal.fail().message("下单失败");
        }
        //c.删除预购单信息
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).delete(userId);
        //d.在redis中把用户购买的商品信息放里面 用于判断该用户是否购买过该商品
        redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).put(userId,orderId);
        return RetVal.ok(orderId);
    }

}

