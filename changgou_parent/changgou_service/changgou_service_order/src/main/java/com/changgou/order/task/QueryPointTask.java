package com.changgou.order.task;

import com.alibaba.fastjson.JSON;
import com.changgou.order.config.RabbitMQConfig;
import com.changgou.order.dao.TaskMapper;
import com.changgou.order.pojo.Task;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
@Component
public class QueryPointTask {
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Scheduled(cron = "0/2 * * * * ?")
    public void QueryTask(){
        //1.查询小于当前系统时间的数据
        List<Task> taskList = taskMapper.findTaskLessThanCurrentTime(new Date());
        if (taskList!=null&&taskList.size()>0){
            for (Task task : taskList) {
                //将数据发送到mq消息队列中
                rabbitTemplate.convertAndSend(RabbitMQConfig.EX_BUYING_ADDPOINTURSE,RabbitMQConfig.CG_BUYING_ADDPOINT_KEY, JSON.toJSONString(task));

                System.out.println("订单服务向积分队列发送了一条信息");
            }
        }
    }
}
