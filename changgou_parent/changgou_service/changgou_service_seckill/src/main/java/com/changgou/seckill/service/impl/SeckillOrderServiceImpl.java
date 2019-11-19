package com.changgou.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.config.ConfirmMessageSender;
import com.changgou.seckill.config.RabbitMQConfig;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.service.SeckillOrderService;
import com.changgou.util.IdWorker;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;

    @Autowired
    private ConfirmMessageSender confirmMessageSender;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    public static final String SECKILL_GOODS_KEY="seckill_goods_";
    public static final String SECKILL_GOODS_STOCK_COUNT_KEY="seckill_goods_stock_count_";
    /**
     * 秒杀下单
     * @param id 商品id
     * @param time 秒杀时间段
     * @param username 登录人姓名
     * @return
     */
    @Override
    public Boolean add(Long id, String time, String username) {
        /**
         * 获取秒杀商品数据与库存量数据，如果没有库存则抛出异常
         *
         * 执行redis预扣减库存,并获取扣减之后的库存值
         *
         * 如果扣减完的库存值<=0, 则删除redis中对应的商品信息与库存信息
         *
         * 基于mq异步方式完成与mysql数据同步
         */

        //防止用户恶意刷单
        this.preventRepeatCommit(username, id);

        //防止相同商品重复购买
        SeckillOrder result = seckillOrderMapper.getSeckillOrderByUsernameAndGoodsId(username, id);
        if (result!=null){
            return false;
        }

        //获取商品数据
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(SECKILL_GOODS_KEY + time).get(id);
        //获取库存信息
        String redisStock= (String) redisTemplate.opsForValue().get(SECKILL_GOODS_STOCK_COUNT_KEY+id);
        //判断是否有库存
        if(StringUtils.isEmpty(redisStock)){
            return false;
        }
        int stock = Integer.parseInt(redisStock);
        if (seckillGoods==null||stock<=0){
            return false;
        }
        //redis预减库存,并获取扣减之后的库存值
        //decrement 减 increment 加 lua脚本语言
        Long count = redisTemplate.opsForValue().decrement(SECKILL_GOODS_STOCK_COUNT_KEY + id);

        if (count<=0){
            //扣减后没有库存,删除redis中的商品信息与库存信息
            redisTemplate.boundHashOps(SECKILL_GOODS_KEY+time).delete(id);

            redisTemplate.delete(SECKILL_GOODS_STOCK_COUNT_KEY + id);
        }

        //消息体:秒杀订单 (有库存,创建秒杀商品订单)
        SeckillOrder seckillOrder=new SeckillOrder();

        seckillOrder.setId(idWorker.nextId());

        seckillOrder.setSeckillId(id);

        seckillOrder.setMoney(seckillGoods.getCostPrice());

        seckillOrder.setUserId(username);

        seckillOrder.setCreateTime(new Date());

        seckillOrder.setSellerId(seckillGoods.getSellerId());
        seckillOrder.setStatus("0");

        //发送消息(保证消息生产者对于消息的不丢失实现)
        confirmMessageSender.sendMessage("", RabbitMQConfig.SECKILL_ORDER_KEY, JSON.toJSONString(seckillOrder));
        return true;
    }
    //防止重复提交
    private String preventRepeatCommit(String username,Long id){
        String redisKey="seckill_user"+username+"_id_"+id;
        long count = redisTemplate.opsForValue().increment(redisKey, 1);
        if (count==1){
            //用户第一次访问
            //设置有效期为5分钟
            redisTemplate.expire(redisKey,5, TimeUnit.MINUTES);
            return "success";
        }
        if (count>1){
            return "fail";
        }
        return "fail";
    }

}
