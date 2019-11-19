package com.changgou.order.listener;

import com.alibaba.fastjson.JSON;
import com.changgou.order.config.RabbitMQConfig;
import com.changgou.order.pojo.Task;
import com.changgou.order.service.TaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DelTaskListener {

    @Autowired
    private TaskService taskService;

    @RabbitListener(queues = RabbitMQConfig.CG_BUYING_FINISHADDPOINT)
    public void receiveDelTask(String message){
        System.out.println("订单服务接受到了删除任何操作的信息");
        Task task = JSON.parseObject(message, Task.class);
        //删除原有的任务数据,并向历史任务表中添加记录
        taskService.delTask(task);

    }
}
