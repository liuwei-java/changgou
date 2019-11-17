package com.itheima;

import com.github.wxpay.sdk.MyConfig;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConfig;

import java.util.HashMap;
import java.util.Map;

public class WxPayTest {
    public static void main(String[] args) throws Exception {

        MyConfig myConfig=new MyConfig();
        WXPay wxPay=new WXPay(myConfig);
        Map<String,String> map=new HashMap<String, String>();

            map.put("body","2345");
            map.put("out_trade_no","20191123606125346");
            map.put("total_fee","1");
            map.put("spbill_create_ip","127.0.0.1");
            map.put("notify_url","http://www.baidu.com");
            map.put("trade_type","NATIVE");
        Map<String, String> result = wxPay.unifiedOrder(map);
        System.out.println(result);


    }
}
