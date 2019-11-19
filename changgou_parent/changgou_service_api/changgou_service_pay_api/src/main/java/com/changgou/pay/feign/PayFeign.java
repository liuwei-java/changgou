package com.changgou.pay.feign;

import com.changgou.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="pay")
public interface PayFeign {
    //下单
    @GetMapping("/wxpay/nativePay")
    public Result nativePay(@RequestParam("orderId") String orderId, @RequestParam("money") Integer money);
    /**
     * 基于微信api查询订单状态
     * @param orderId
     * @return
     */
    @GetMapping("/wxpay/query/{orderId}")
    public Result queryOrder(@PathVariable("orderId") String orderId);
    /**
     * 基于微信api关闭订单
     * @param orderId
     * @return
     */
    @GetMapping("/close/{orderId}")
    public Result closeOrder(@PathVariable("orderId") String orderId);
}
