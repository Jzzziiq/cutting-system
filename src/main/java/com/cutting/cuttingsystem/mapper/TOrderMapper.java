package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TOrderMapper extends BaseMapper<TOrder> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_order
            WHERE order_id = #{orderId}
            """)
    TOrder selectByIdIgnoreTenant(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("DELETE FROM t_order WHERE order_id = #{orderId}")
    int deleteByIdIgnoreTenant(@Param("orderId") Long orderId);
}
