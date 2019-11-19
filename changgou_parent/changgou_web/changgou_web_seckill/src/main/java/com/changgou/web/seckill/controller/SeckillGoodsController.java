package com.changgou.web.seckill.controller;

import com.changgou.entity.Result;
import com.changgou.seckill.feign.SeckillGoodsFeign;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/wseckillgoods")
public class SeckillGoodsController {
    @Autowired
    private SeckillGoodsFeign seckillGoodsFeign;

    @RequestMapping("/toIndex")
    public String toIndex(){
        return "seckill-index";
    }

    @RequestMapping("/timeMenus")
    @ResponseBody
    public List<String> timeMenus(){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //获取当前时间段相关信息集合
        List<Date> dateMenus = DateUtil.getDateMenus();
         List<String> result =new ArrayList<>();
        for (Date dateMenu : dateMenus) {
            String format = simpleDateFormat.format(dateMenu);
            result.add(format);
        }
        return result;
    }

    @RequestMapping("/list")
    @ResponseBody
    public Result<List<SeckillGoods>> list(String time){
        Result<List<SeckillGoods>> listRsult = seckillGoodsFeign.list(DateUtil.formatStr(time));
        return listRsult;

    }
}
