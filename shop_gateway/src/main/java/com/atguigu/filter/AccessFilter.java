package com.atguigu.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

//这是一个全局拦截器
@Component
public class AccessFilter implements GlobalFilter {
    @Value("${filter.whiteList}")
    public String filterWhiteList;
    //addCart.html,myOrder.html

    @Autowired
    private RedisTemplate redisTemplate;
    //匹配路径的对象
    private AntPathMatcher antPathMatcher=new AntPathMatcher();
    /**
     *
     * @param exchange 服务网络交换机 存储请求和响应消息 它是一个不可变的实例
     * @param chain 网关过滤链表 用于链式调用
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        //4.拿到用户的临时id(在未登录的情况下使用)
        String userTempId= getUserTempId(request);
        //3.如果用户登录成功之后 可以拿到userId
        String userId= getUserId(request);

        //1.内部接口 不允许直接访问
        if(antPathMatcher.match("/sku/**",path)){
            //写信息给浏览器
            return writeDataToBrowser(exchange);

        }
        //2.对于请求某些资源(请求白名单) 必须要先登录
        for (String filterWhite : filterWhiteList.split(",")) {
            //说明请求路径在白名单范围内 用户未登录
            if(path.indexOf(filterWhite)!=-1&&StringUtils.isEmpty(userId)){
                //让它跳转到登录页面
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originalUrl="+request.getURI());
                return response.setComplete();
            }
        }
        //4.把用户信息保存到header中传给shop-web那边的request
        if(!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){
            if(!StringUtils.isEmpty(userId)){
                request.mutate().header("userId",userId).build();
            }
            if(!StringUtils.isEmpty(userTempId)){
                request.mutate().header("userTempId",userTempId).build();
            }
            //放开拦截器 让下游继续执行(传递了参数)
            return chain.filter(exchange.mutate().request(request).build());
        }

        //如果不想拦截 放开
        return chain.filter(exchange);
    }

    private String getUserTempId(ServerHttpRequest request) {
        String userTempId="";
        List<String> headerValueList = request.getHeaders().get("userTempId");
        if(!CollectionUtils.isEmpty(headerValueList)){
            userTempId = headerValueList.get(0);
        }else{
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if(cookie!=null){
                userTempId=cookie.getValue();
            }
        }
        return userTempId;
    }

    private String getUserId(ServerHttpRequest request) {
        //获取登录之后的token
        String token="";
        List<String> headerValueList = request.getHeaders().get("token");
        if(!CollectionUtils.isEmpty(headerValueList)){
            token = headerValueList.get(0);
        }else{
            HttpCookie cookie = request.getCookies().getFirst("token");
            if(cookie!=null){
                token=cookie.getValue();
            }
        }
        //通过token获取用户的id
        if(!StringUtils.isEmpty(token)){
            String userKey = "user:login:" + token;
            //此时redis里面本身有一份信息 但是拿不到
            JSONObject loginUserInfoJson=(JSONObject)redisTemplate.opsForValue().get(userKey);
            if(loginUserInfoJson!=null){
                //拿到登录时的ip地址
                String beforeLoginIp = loginUserInfoJson.getString("loginIp");
                //拿到当前的ip地址
                String gatwayIpAddress = IpUtil.getGatwayIpAddress(request);
                if(gatwayIpAddress.equals(beforeLoginIp)){
                   return loginUserInfoJson.getString("userId");
                }
            }
        }
        return null;
    }

    private Mono<Void> writeDataToBrowser(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        //设置返回给浏览器的数据类型 json
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        //写的数据是什么
        RetVal<Object> retVal = RetVal.build(null, RetValCodeEnum.NO_PERMISSION);
        //把数据转换为json
        byte[] bytes = JSONObject.toJSONString(retVal).getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer=response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(dataBuffer));
    }
}
