package com.changgou.goods.dao;

import com.changgou.goods.pojo.Sku;
import com.changgou.order.pojo.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

public interface SkuMapper extends Mapper<Sku> {
    /**
     * 扣减库存增加销量
     * @param orderItem
     * @return
     */
    @Update("update tb_sku set num=num-#{num},sale_num=sale_num+#{num} where id=#{skuId} and num>=#{num}")
    int decrCount(OrderItem orderItem);
    //回滚库存(增加库存,减少销量)

    @Update("update tb_sku set num=num+#{num},sale_num=sale_num-#{num} where id=#{skuId}")
    void recoverStockNum(@Param("skuId") String skuId,@Param("num") Integer num);

}
