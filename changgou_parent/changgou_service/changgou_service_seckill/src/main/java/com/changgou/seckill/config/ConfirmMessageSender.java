package com.changgou.seckill.config;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component

public class ConfirmMessageSender implements RabbitTemplate.ConfirmCallback {

    @Autowired // 存储失败消息
    private RedisTemplate redisTemplate;
    @Autowired //对rabbitTemplate方法进行加强
    private RabbitTemplate rabbitTemplate;

    public static final String MESSAGE_CONFIRM_KEY="message_confirm_";

    public ConfirmMessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate=rabbitTemplate;
        //当前回调函数返回的对象
        rabbitTemplate.setConfirmCallback(this);
    }

    @Override
    //接受消息服务器返回的通知
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack){
            //成功通知，删除redis中的相关数据
            redisTemplate.delete(correlationData.getId());

            //相关元数据
            redisTemplate.delete(MESSAGE_CONFIRM_KEY+correlationData.getId());
        }else{
            //接受失败通知,
            //从redis中获取刚才的消息内容 继续发送
            Map<String,String> map = (Map<String,String>)redisTemplate.opsForHash().entries(MESSAGE_CONFIRM_KEY + correlationData.getId());

            String exchange = map.get("exchange");
            String message = map.get("message");
            String rountingKey = map.get("rountingKey");
            rabbitTemplate.convertAndSend(exchange,rountingKey, JSON.toJSONString(message));
        }
    }

    //自定义消息发送方法
    public void sendMessage(String exchange,String rountingKey,String message){

        //设置消息唯一标识并存入redis
        CorrelationData correlationData=new CorrelationData(UUID.randomUUID().toString());

        redisTemplate.opsForValue().set(correlationData.getId(),message);

        //将相关发送消息的相关元数据保存到redis中
        Map<String,String> map=new HashMap<>();

        map.put("exchange",exchange);
        map.put("rountingKey",rountingKey);
        map.put("message",message);

        redisTemplate.opsForHash().putAll(MESSAGE_CONFIRM_KEY+correlationData.getId(),map);

        //携带唯一标识发送消息

        rabbitTemplate.convertAndSend(exchange,rountingKey,message,correlationData);

    }
}
