package com.changgou.seckill.service;

import com.changgou.seckill.pojo.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {
    /**
     * 查询redis中的秒杀商品
     * @param time
     * @return
     */
    List<SeckillGoods> list(String time);
}
