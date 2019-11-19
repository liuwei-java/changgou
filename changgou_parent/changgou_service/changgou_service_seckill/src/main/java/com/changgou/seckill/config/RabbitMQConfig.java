package com.changgou.seckill.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConfig {
    //秒杀商品订单消息
    public static final String SECKILL_ORDER_KEY="seckill_order_";

    @Bean
    public Queue queue(){
        //开启队列持久化
        return new Queue(SECKILL_ORDER_KEY,true);
    }
}
