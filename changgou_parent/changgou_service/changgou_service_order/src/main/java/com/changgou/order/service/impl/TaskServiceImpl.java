package com.changgou.order.service.impl;

import com.changgou.order.dao.TaskHisMapper;
import com.changgou.order.dao.TaskMapper;
import com.changgou.order.pojo.Task;
import com.changgou.order.pojo.TaskHis;
import com.changgou.order.service.TaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
@Service
public class TaskServiceImpl implements TaskService {
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private TaskHisMapper taskHisMapper;
    @Override
    public void delTask(Task task) {
        //1.设置删除时间
      task.setDeleteTime(new Date());
        Long taskId= task.getId();
        task.setId(null);

        //bean复制
        TaskHis taskHis=new TaskHis();
        BeanUtils.copyProperties(task,taskHis);
        System.out.println(taskHis);

        //记录任务信息
       taskHisMapper.insertSelective(taskHis);

        //删除原有操作
        task.setId(taskId);
        taskMapper.deleteByPrimaryKey(task);
        System.out.println("当前订单服务完成历史添加任务,并删除原有任务的操作");
    }
}
