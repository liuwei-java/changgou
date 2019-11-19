package com.changgou.web.seckill.controller;

import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.seckill.feign.SeckillOrderFeign;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.util.RandomUtil;
import com.changgou.web.seckill.util.CookieUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/wseckillorder")
public class SeckillOrderController {
    @Autowired
    private SeckillOrderFeign seckillOrderFeign;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping("/add")
    public Result<List<SeckillOrder>> add(@RequestParam("time") String time, @RequestParam("id") Long id,String random){
        String cookieValue = this.readCookie();
        //校验密文有效
        String redisRandom= (String) redisTemplate.opsForValue().get("randomcode_"+cookieValue);
        if (StringUtils.isEmpty(redisRandom)){
            return new Result<>(false, StatusCode.ERROR,"下单失败");
        }
        if (random.equals(redisRandom)){
            return new Result<>(false, StatusCode.ERROR,"下单失败");
        }

        Result result = seckillOrderFeign.add(time, id);
        return result;
    }
    @GetMapping("/getToken")
    @ResponseBody
    public String getToken(){
        String randomString = RandomUtil.getRandomString();

        String cookieValue = this.readCookie();

        redisTemplate.opsForValue().set("randomcode_"+cookieValue,randomString,5, TimeUnit.SECONDS);

        return randomString;
    }

    private String readCookie(){

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String jti = CookieUtil.readCookie(request, "uid").get("uid");
        return jti;
    }
}
