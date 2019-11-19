package com.changgou.pay.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.pay.config.RabbitMQConfig;
import com.changgou.pay.service.WxPayService;
import com.changgou.util.ConvertUtils;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wxpay")
public class WxPayController {
    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //下单
    @GetMapping("/nativePay")
    public Result nativePay(@RequestParam("orderId") String orderId, @RequestParam("money") Integer money){

        Map result = wxPayService.nativePay(orderId, money);
        return new Result(true, StatusCode.OK,"二维码生成",result);
    }
    //回调
    @RequestMapping("/notify")
    public void notifyLogic(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        System.out.println("支付成功回调");
        try {

        //输入流转化为字符串
            String xml = ConvertUtils.convertToString(httpServletRequest.getInputStream());
            System.out.println(xml);

            //基于微信发送内容,完成后续
            Map<String, String> result = WXPayUtil.xmlToMap(xml);
            //查询订单
            if("SUCCESS".equals(result.get("result_code"))){
                Map map = wxPayService.queryOrder(result.get("out_trade_no"));
                System.out.println("查询订单结果:"+map);

                //查询结果成功后,发送消息到rabbitMq
                if ("SUCCESS".equals(map.get("result_code"))){
                        //订单id 交易id
                    Map message=new HashMap();
                    message.put("transactionId",map.get("transaction_id"));
                    message.put("orderId",map.get("out_trade_no"));

                    //发送消息
                    rabbitTemplate.convertAndSend("", RabbitMQConfig.ORDER_PAY, JSON.toJSONString(message));

                    //完成双向通信
                    rabbitTemplate.convertAndSend("paynotify","",map.get("out_trade_no"));
                }else{
                    //输出错误原因
                    System.out.println(result.get("err_code_des"));
                }

            }else{
                //输出错误原因
                System.out.println(result.get("err_code_des"));
            }

            //给微信一个结果通知
            httpServletResponse.setContentType("text/xml");
            String data="<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
            httpServletResponse.getWriter().write(data);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 基于微信api查询订单状态
     * @param orderId
     * @return
     */
    @GetMapping("/query/{orderId}")
    public Result queryOrder(@PathVariable("orderId") String orderId){
        Map resultMap = wxPayService.queryOrder(orderId);
        return new Result(true,StatusCode.OK,"查询订单成功",resultMap);

    }
    /**
     * 基于微信api关闭订单
     * @param orderId
     * @return
     */
    @GetMapping("/close/{orderId}")
    public Result closeOrder(@PathVariable("orderId") String orderId){
        Map resultMap = wxPayService.queryOrder(orderId);
        return new Result(true,StatusCode.OK,"关闭订单成功",resultMap);

    }
}
