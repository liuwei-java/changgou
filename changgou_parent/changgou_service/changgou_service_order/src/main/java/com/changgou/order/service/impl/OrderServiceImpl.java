package com.changgou.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import com.changgou.entity.Contant;
import com.changgou.entity.Result;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.order.config.RabbitMQConfig;
import com.changgou.order.dao.*;
import com.changgou.order.pojo.*;
import com.changgou.order.service.CartService;
import com.changgou.order.service.OrderService;
import com.changgou.pay.feign.PayFeign;
import com.changgou.util.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.websocket.server.PathParam;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private CartService cartService;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private OrderLogMapper orderLogMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 查询全部列表
     * @return
     */
    @Override
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public Order findById(String id){
        return  orderMapper.selectByPrimaryKey(id);
    }


    /**
     * 增加
     * @param order
     */
    @Override
    @GlobalTransactional(name = "order_add")
    public String add(Order order){
        // 1.获取购物车相关数据(redis)
        Map cartMap = cartService.list(order.getUsername());
        List<OrderItem> orderItemList= (List<OrderItem>) cartMap.get("orderItemList");

        // 2.统计计算:总金额 总数量
        order.setTotalMoney((Integer) cartMap.get("totalPrice"));
        order.setTotalNum((Integer) cartMap.get("totalNum"));
        order.setPayMoney((Integer) cartMap.get("totalPrice"));
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());

        order.setBuyerRate(Contant.ORDER_ONT_RATE);//是否评价
        order.setConsignStatus(Contant.NOT_YET_SHIPPED);//未发货
        order.setSourceType(Contant.SOURCE_OF_WEB_ORDER);
        order.setOrderStatus(Contant.ORDER_UNFINISHED);
        order.setPayStatus(Contant.ORDER_NOT_PAY);//未支付
        String orderId = idWorker.nextId() + "";
        order.setId(orderId);
        // 3.填充订单数据保存到tb_order
        orderMapper.insertSelective(order);
        // 4.填充订单项数据并保存到tb_order_item
        for (OrderItem orderItem : orderItemList) {
            orderItem.setId(idWorker.nextId()+"");
            orderItem.setIsReturn(Contant.IS_NOT_RETURN);
            orderItem.setOrderId(order.getId());
            orderItemMapper.insertSelective(orderItem);
        }
        //扣减库存增加销量
        skuFeign.decrCount(order.getUsername());
        //int i=1/0;
        //添加任务数据
        System.out.println("向订单数据库中的任务数据表去添加任务数据");
        Task task=new Task();
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        task.setMqExchange(RabbitMQConfig.EX_BUYING_ADDPOINTURSE);
        task.setMqRoutingkey(RabbitMQConfig.CG_BUYING_ADDPOINT_KEY);
        Map map=new HashMap();
        map.put("username",order.getUsername());//封装requestBody 用户名
        map.put("orderId",order.getId()); //订单id
        map.put("point",order.getPayMoney());//积分
        task.setRequestBody(JSON.toJSONString(map));
        //将task保存到数据库
        taskMapper.insertSelective(task);

        // 5.删除redis缓存的购物车数据
        redisTemplate.delete("Cart_"+order.getUsername());

        //发送延迟消息
        rabbitTemplate.convertAndSend("","queue.ordercreate",orderId);
        return orderId;

    }


    /**
     * 修改
     * @param order
     */
    @Override
    public void update(Order order){
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(String id){
        orderMapper.deleteByPrimaryKey(id);
    }


    /**
     * 条件查询
     * @param searchMap
     * @return
     */
    @Override
    public List<Order> findList(Map<String, Object> searchMap){
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<Order> findPage(int page, int size){
        PageHelper.startPage(page,size);
        return (Page<Order>)orderMapper.selectAll();
    }

    /**
     * 条件+分页查询
     * @param searchMap 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Order> findPage(Map<String,Object> searchMap, int page, int size){
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        return (Page<Order>)orderMapper.selectByExample(example);
    }
    /**
     * 修改订单状态为已支付,并记录日志
     * @param orderId
     * @param transactionId
     */
    @Override
    public void updatePayStatus(String orderId, String transactionId) {
        //查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        //System.out.println("订单修改之前的支付状态:"+order.getPayStatus());
        //修改订单支付状态
        if (order!=null&&"0".equals(order.getPayStatus())){
            order.setPayStatus("1");//修改订单状态为已支付
            order.setOrderStatus("1");//
            order.setTransactionId(transactionId);//微信返回的交易流水号
            order.setUpdateTime(new Date());
            order.setPayTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);

            //记录日志
            OrderLog orderLog=new OrderLog();
            orderLog.setId(idWorker.nextId()+"");
            orderLog.setOperater("system");
            orderLog.setOperateTime(new Date());
            orderLog.setConsignStatus("0");
            orderLog.setOrderId(orderId);
            orderLog.setOrderStatus("1");
            orderLog.setPayStatus("1");
            orderLog.setRemarks("交易流水号:"+transactionId);
            orderLogMapper.insert(orderLog);

        }
    }
    @Autowired
    private PayFeign payFeign;
    /**
     * 关闭订单
     * @param orderId
     */
    @Override
    @Transactional
    public void closeOrder(String orderId) {
        /**
         * 1.根据订单id查询mysql的订单信息,判断订单是否存在,判断订单的状态
         * 2.基于微信查询订单信息
         * 2.1已支付,进行sql数据补偿
         * 2.2未支付,修改数据库订单信息,增加订单日志,恢复商品库存,基于微信关闭订单
         */
        System.out.println("关闭订单业务开启:"+orderId);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order==null){
            throw new RuntimeException("订单不存在");
        }
        if(!"0".equals(order.getPayStatus())){
            throw new RuntimeException("当前订单无需关闭");
        }
        System.out.println("关闭订单校验通过:"+orderId);

        //基于微信查询订单信息
        Map wxQueryMap = (Map) payFeign.queryOrder(orderId).getData();
        System.out.println("查询微信支付订单:"+wxQueryMap);
        if ("SUCCESS".equals(wxQueryMap.get("trade_state"))){
            //已支付进行数据补偿
            String transactionId = (String) wxQueryMap.get("transaction_id");
            this.updatePayStatus(orderId,transactionId);
            System.out.println("完成数据补偿");
        }
        //未支付,修改数据库订单信息,增加订单日志,恢复商品库存,基于微信关闭订单
        if ("NOTPAY".equals(wxQueryMap.get("trade_state"))){

            System.out.println("执行关闭");
            order.setCloseTime(new Date());
            order.setOrderStatus("4");
            orderMapper.updateByPrimaryKeySelective(order);
            //记录更新日志
            OrderLog orderLog=new OrderLog();
            orderLog.setId(idWorker.nextId()+"");
            orderLog.setOperater("system");
            orderLog.setOperateTime(new Date());
            orderLog.setOrderStatus("4");
            orderLogMapper.insert(orderLog);
            //恢复库存和销量
            OrderItem item=new OrderItem();
            item.setId(orderId);
            List<OrderItem> orderItemList = orderItemMapper.select(item);
            for (OrderItem orderItem : orderItemList) {
                skuFeign.recoverStockNum(orderItem.getSkuId(),orderItem.getNum());
            }
            //基于微信关闭订单
            payFeign.closeOrder(orderId);

        }
    }

    /**
     * 批量发货
     * @param orders
     */
    @Override
    @Transactional
    public void bathSend(List<Order> orders) {
        //判断运单号和物流公司是否为空
        for (Order order : orders) {
            if (order.getShippingCode()==null||order.getShippingName()==null){
                throw new RuntimeException("请选择快递公司和填写快递单号");
            }
            if (order.getId()==null){
                throw new RuntimeException("该订单不存在");
            }
        }
        //进行订单状态的校验
        for (Order order : orders) {
            Order order1 = orderMapper.selectByPrimaryKey(order.getId());
            if (!"0".equals(order1.getConsignStatus())||!"1".equals(order1.getOrderStatus())){
                throw new RuntimeException("订单状态有误！");
            }
        }

        //修改订单的状态为已发货
        for (Order order : orders) {
            order.setOrderStatus("2");
            order.setConsignStatus("1");
            order.setUpdateTime(new Date());
            order.setConsignTime(new Date());
            orderMapper.updateByPrimaryKeySelective(order);

            //更新订单变动日志
            OrderLog orderLog=new OrderLog();
            orderLog.setId(idWorker.nextId()+"");
            orderLog.setOperater("admin");
            orderLog.setOperateTime(new Date());
            orderLog.setConsignStatus("1");
            orderLog.setOrderStatus("2");
            orderLog.setOrderId(order.getId());
            orderLogMapper.insert(orderLog);

        }

    }

    @Override
    @Transactional
    public void confirmTask(String orderId, String operator) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order==null){
            throw new RuntimeException("订单不存在");
        }
        if (!"1".equals(order.getConsignStatus())){
            throw new RuntimeException("订单未发货");
        }
        order.setConsignStatus("2");//已送达
        order.setOrderStatus("3");//已完成
        order.setUpdateTime(new Date());
        order.setEndTime(new Date());
        orderMapper.updateByPrimaryKeySelective(order);
        //记录变动日志
        OrderLog orderLog=new OrderLog();
        orderLog.setId(idWorker.nextId()+"");
        orderLog.setOperater("admin");
        orderLog.setOperateTime(new Date());
        orderLog.setOrderStatus("3");
        orderLog.setOrderId(order.getId());
        orderLogMapper.insertSelective(orderLog);
    }

    @Autowired
    private OrderConfigMapper orderConfigMapper;

    /**
     * 自动收货
     */
    @Override
    @Transactional
    public void autoTask() {
        /**
         * 从订单配置表中获取订单自动确认期限
         * 得到当前日期向前数(订单自动确认)天,作为过期时间节点
         * 从订单表中获取订单,满足发货时间小于过期时间,且未收货
         */
        //获取订单配置信息
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey("1");

        //获取时间节点
        LocalDate now = LocalDate.now();

        //获取过期的时间节点,在这个日期前发货的未收货订单
        LocalDate date = now.plusDays(-orderConfig.getServiceTimeout());
        System.out.println(date);

        //按条件查询过期订单
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("consignTime",date);
        criteria.andEqualTo("orderStatus","2");
        List<Order> orders = orderMapper.selectByExample(example);
        for (Order order : orders) {
            this.confirmTask(order.getId(),"system");
        }

    }

    /**
     * 构建查询对象
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andEqualTo("id",searchMap.get("id"));
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andEqualTo("payType",searchMap.get("payType"));
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andEqualTo("sourceType",searchMap.get("sourceType"));
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andEqualTo("orderStatus",searchMap.get("orderStatus"));
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andEqualTo("payStatus",searchMap.get("payStatus"));
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andEqualTo("consignStatus",searchMap.get("consignStatus"));
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andEqualTo("isDelete",searchMap.get("isDelete"));
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}

