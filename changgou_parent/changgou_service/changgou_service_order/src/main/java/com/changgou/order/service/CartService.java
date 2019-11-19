package com.changgou.order.service;

import java.util.Map;

public interface CartService {
    /**
     * 添加购物车
     * @param skuId
     * @param num
     * @param username
     */
    void addCart(String skuId,Integer num,String username);

    /**
     * 查询购物车数据
     * @param username
     * @return
     */
    Map list(String username);
}
