package com.changgou.order.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class RabbitMQConfig {
    //添加积分任务交换机
    public static final String EX_BUYING_ADDPOINTURSE = "ex_buying_addpointurse";

    //添加积分消息队列
    public static final String CG_BUYING_ADDPOINT = "cg_buying_addpoint";

    //完成添加积分消息队列
    public static final String CG_BUYING_FINISHADDPOINT = "cg_buying_finishaddpoint";

    //添加积分路由key
    public static final String CG_BUYING_ADDPOINT_KEY = "addpoint";

    //完成添加积分路由key
    public static final String CG_BUYING_FINISHADDPOINT_KEY = "finishaddpoint";
    //订单支付队列
    public static final String ORDER_PAY="order_pay";

    public static final String ORDER_TACK="order_tack";
    //声明队列
    @Bean(ORDER_PAY)
    public Queue queue(){
        return new Queue(ORDER_PAY);
    }

    @Bean(CG_BUYING_ADDPOINT)
    public Queue CG_BUYING_ADDPOINT(){
        Queue queue=new Queue(CG_BUYING_ADDPOINT);
        return queue;
    }
    //声明队列
    @Bean(CG_BUYING_FINISHADDPOINT)
    public Queue CG_BUYING_FINISHADDPOINT(){
        Queue queue=new Queue(CG_BUYING_FINISHADDPOINT);
        return queue;
    }
    //声明交换机
    @Bean(EX_BUYING_ADDPOINTURSE)
    public Exchange EX_BUYING_ADDPOINTURSE(){
        return ExchangeBuilder.directExchange(EX_BUYING_ADDPOINTURSE).durable(true).build();
    }
    //绑定队列到交换机
    @Bean
    public Binding BINDING_CG_BUYING_ADDPOINT(@Qualifier(CG_BUYING_ADDPOINT) Queue queue,@Qualifier(EX_BUYING_ADDPOINTURSE) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(CG_BUYING_ADDPOINT_KEY).noargs();
    }
    //绑定队列到交换机
    @Bean
    public Binding BINDING_CG_BUYING_FINISHADDPOINT(@Qualifier(CG_BUYING_FINISHADDPOINT) Queue queue,@Qualifier(EX_BUYING_ADDPOINTURSE) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(CG_BUYING_FINISHADDPOINT_KEY).noargs();
    }
    @Bean
    public Queue ORDER_TACK(){
        return new Queue(ORDER_TACK);
    }
}
