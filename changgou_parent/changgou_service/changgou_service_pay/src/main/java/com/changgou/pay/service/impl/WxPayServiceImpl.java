package com.changgou.pay.service.impl;

import com.changgou.pay.service.WxPayService;
import com.github.wxpay.sdk.WXPay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
@Service
public class WxPayServiceImpl implements WxPayService {
    @Autowired
    private WXPay wxPay;
  @Value("${wxpay.notify_url}")
  private String notify_url;
    @Override
    public Map nativePay(String orderId, Integer money) {

        try{
            Map map=new HashMap();
            //封装请求参数
            map.put("body","heima");
            map.put("out_trade_no",orderId);
            BigDecimal payMoney=new BigDecimal("0.01");
            BigDecimal fen = payMoney.multiply(new BigDecimal("100"));//乘法运算
            fen=fen.setScale(0,BigDecimal.ROUND_UP);//向上取整
            map.put("total_fee",String.valueOf(fen));
            map.put("spbill_create_ip","127.0.0.1");
            map.put("notify_url",notify_url);
            map.put("trade_type","NATIVE");

            //基于wxPay完成统一下单接口的调用,并返回结果
            Map<String,String> result = wxPay.unifiedOrder(map);
            return result;

        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 查询订单
     * @param orderId
     * @return
     */
    @Override
    public Map queryOrder(String orderId) {
        Map<String,String> map=new HashMap<>();
        map.put("out_trade_no",orderId);
        try {
            Map<String, String> resultMap = wxPay.orderQuery(map);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 基于微信关闭订单
     * @param orderId
     * @return
     */
    @Override
    public Map closeOrder(String orderId) {
        Map<String,String> map=new HashMap();
        map.put("out_trade_no",orderId);
        try{

            Map<String, String> resultMap = wxPay.closeOrder(map);
           return resultMap;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

    }
}
