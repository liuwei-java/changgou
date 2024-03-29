package com.changgou.seckill.service.impl;

import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.service.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    @Autowired
    private RedisTemplate redisTemplate;
    private static final String SECKILL_GOODS_KEY="seckill_goods_";
    /**
     * 查询redis中的秒杀商品
     * @param time
     * @return
     */
    @Override
    public List<SeckillGoods> list(String time) {
        List list = redisTemplate.boundHashOps(SECKILL_GOODS_KEY + time).values();

        return list;
    }
}
