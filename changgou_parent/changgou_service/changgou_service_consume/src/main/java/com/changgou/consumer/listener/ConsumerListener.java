package com.changgou.consumer.listener;

import com.alibaba.fastjson.JSON;
import com.changgou.consumer.config.RabbitMQConfig;
import com.changgou.consumer.service.SecKillOrderService;
import com.changgou.seckill.pojo.SeckillOrder;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ConsumerListener {
    @Autowired
    private SecKillOrderService secKillOrderService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_KEY)
    public void receiceSeckillOrderMessage(Message message, Channel channel){
        //设置预抓取总数
        try {
            channel.basicQos(300);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //转换消息
        SeckillOrder seckillOrder= JSON.parseObject(message.getBody(), SeckillOrder.class);

        //基于业务层完成与mysql数据同步
        int result = secKillOrderService.createOrder(seckillOrder);
        if (result>0){
            //返回成功通知
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
