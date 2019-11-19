package com.changgou.web.order.controller;

import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.order.feign.CartFeign;
import com.changgou.order.feign.OrderFeign;
import com.changgou.order.pojo.Order;
import com.changgou.order.pojo.OrderItem;
import com.changgou.user.feign.AddressFeign;
import com.changgou.user.pojo.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/worder")
public class OrderController {
    @Autowired
    private CartFeign cartFeign;
    @Autowired
    private AddressFeign addressFeign;
    @Autowired
    private OrderFeign orderFeign;
    @RequestMapping("/ready/order")
    public String readyOrder(Model model){
        //获取收件人信息
        List<Address> addressList = addressFeign.findAddress().getData();
        model.addAttribute("addressList",addressList);

        //获取购物车信息
        Map map = cartFeign.list();
        List<OrderItem> orderItemList= (List<OrderItem>) map.get("orderItemList");
        Integer totalPrice= (Integer) map.get("totalPrice");
        Integer totalNum= (Integer) map.get("totalNum");
        model.addAttribute("carts",orderItemList);
        model.addAttribute("totalPrice",totalPrice);
        model.addAttribute("totalNum",totalNum);
        //默认收件人信息
        for (Address address : addressList) {
            if (address.getIsDefault().equals("1")){
                model.addAttribute("deAddr",address);
                break;
            }
        }
        return "order";
    }
    @PostMapping("/add")
    @ResponseBody
    public Result add(@RequestBody Order order) {
        return orderFeign.add(order);
    }
    @GetMapping("/toPayPage")
    public String toPayPage (String orderId,Model model){
        //根据id查询订单 需要调用orderFeign远程调用该方法
        //获取订单相关信息
        Result<Order> orderResult = orderFeign.findById(orderId);
        Order order = orderResult.getData();
        model.addAttribute("orderId",orderId);
        model.addAttribute("payMoney",order.getPayMoney());
        return "pay";
    }

}
