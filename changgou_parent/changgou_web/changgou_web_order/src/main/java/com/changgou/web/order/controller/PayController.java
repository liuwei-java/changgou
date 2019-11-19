package com.changgou.web.order.controller;

import com.changgou.entity.Result;
import com.changgou.order.feign.OrderFeign;
import com.changgou.order.pojo.Order;
import com.changgou.pay.feign.PayFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import java.util.Map;

@Controller
@RequestMapping("/wxpay")
public class PayController {
    @Autowired
    private OrderFeign orderFeign;
    @Autowired
    private PayFeign payFeign;

    //跳转到微信支付二维码页面
    @GetMapping
    public String WxPay(String orderId, Model model){

        //根据orderId查询订单状态 ,不存在,错误页面
        Result<Order> orderResult = orderFeign.findById(orderId);
        if (orderResult.getData()==null){
            return "fail";
        }
        //支付状态不是未支付.错误页面
        Order order = orderResult.getData();
            if (!"0".equals(order.getPayStatus())){
                return "fail";
            }
        //基于payFeign调用统计下单接口,返回结果
        Result payResult = payFeign.nativePay(orderId, order.getPayMoney());
            if (payResult==null){
                return "fail";
            }

        //封装结果数据
        Map data = (Map) payResult.getData();
            data.put("payMoney",order.getPayMoney());//页面需要信息
            data.put("orderId",orderId);
            model.addAllAttributes(data);
        return "wxpay";
    }

    /**
     * 支付成功跳转页面
     * @param payMoney
     * @param model
     * @return
     */
    @GetMapping("/toPaySuccess")
    public String toPaySuccess(Integer payMoney,Model model){
        model.addAttribute("payMoney",payMoney);
        return "paysuccess";
    }
}
