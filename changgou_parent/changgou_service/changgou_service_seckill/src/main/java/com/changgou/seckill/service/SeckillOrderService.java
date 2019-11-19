package com.changgou.seckill.service;

public interface SeckillOrderService {
    /**
     * 秒杀下单
     * @param id
     * @param time
     * @param username
     * @return
     */
    Boolean add(Long id, String time, String username);

}
