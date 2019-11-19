package com.changgou.order.service.impl;

import com.changgou.entity.Result;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

public class CartServiceImpl implements CartService {
    // 0.注入redisTemplate
   @Autowired
   private RedisTemplate redisTemplate;
   @Autowired
   private SkuFeign skuFeign;
   @Autowired
   private SpuFeign spuFeign;

    private static final String CART="Cart_";

    /**
     * 添加购物车
     * @param skuId
     * @param num
     * @param username
     */
    @Override
    public void addCart(String skuId, Integer num, String username) {

        // 1.查询redis对应的商品信息(hash类型保存, key field value ->key map)
       //cart_username skuid orderItem(购物车中的每一个商品)
        OrderItem  orderItem = (OrderItem) redisTemplate.boundHashOps(CART + username).get(skuId);

        if (orderItem!=null){
            // 2.若当前商品在redis中存在,更新商品的价格与数量
            orderItem.setNum(orderItem.getNum()+num);
            orderItem.setMoney(orderItem.getNum()*orderItem.getPrice());
            orderItem.setPayMoney(orderItem.getNum()*orderItem.getPrice());
        }else{

            //先获取sku对象
            Sku sku = skuFeign.findById(skuId).getData();

            //获取spu
            Spu spu = spuFeign.findSpuById(sku.getSpuId()).getData();
            //将sku转换成orderItem
            orderItem =this.sku2OrderItem(sku,spu,num);
        }
        // 3.redis中不存在,将商品添加到redis中
        redisTemplate.boundHashOps(CART+username).put(skuId,orderItem);
        }

    @Override
    public Map list(String username) {
        Map map=new HashMap();

        List<OrderItem> orderItemList = redisTemplate.boundHashOps(CART + username).values();
        map.put("orderItemList",orderItemList);
        //商品数量与总价格
        Integer totalNum=0;
        Integer totalPrice=0;
        for (OrderItem orderItem : orderItemList) {
             totalPrice += orderItem.getMoney();
             totalNum+=orderItem.getNum();

        }
        map.put("totalNum",totalNum);
        map.put("totalPrice",totalPrice);
        return map;
    }

    private OrderItem sku2OrderItem(Sku sku, Spu spu, Integer num){
        //OrderItem实体类
        OrderItem orderItem=new OrderItem();

        orderItem.setSkuId(sku.getId());
        orderItem.setSpuId(sku.getSpuId());
        orderItem.setName(sku.getName());
        orderItem.setPrice(sku.getPrice());
        orderItem.setNum(num);

        orderItem.setMoney(num*orderItem.getPrice());//单件*数量
        orderItem.setPayMoney(num*orderItem.getPrice());//实付金额
        orderItem.setImage(sku.getImage());
        orderItem.setWeight(sku.getWeight()*num);//重量=单个重量*数量

        //分类id
        orderItem.setCategoryId1(spu.getCategory1Id());
        orderItem.setCategoryId2(spu.getCategory2Id());
        orderItem.setCategoryId3(spu.getCategory3Id());

        return orderItem;
    }


}
