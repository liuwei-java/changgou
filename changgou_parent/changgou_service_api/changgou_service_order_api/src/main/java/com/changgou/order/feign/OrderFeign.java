package com.changgou.order.feign;

import com.changgou.entity.Result;
import com.changgou.order.pojo.Order;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.websocket.server.PathParam;

@FeignClient(name="order")
public interface OrderFeign {
    /***
     * 新增order数据
     * @param order
     * @return
     */
    @PostMapping("/order")
    public Result add(@RequestBody Order order);

    /***
     * 根据ID查询数据
     * @param id
     * @return
     */
    @GetMapping("/order/{id}")
    public Result<Order> findById(@PathVariable String id);

}

