package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TOrderItemMapper extends BaseMapper<TOrderItem> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_order_item
            WHERE order_id = #{orderId}
            ORDER BY item_id ASC
            """)
    List<TOrderItem> selectByOrderIdIgnoreTenant(@Param("orderId") Long orderId);
}
