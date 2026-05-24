package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TLayoutResultMapper extends BaseMapper<TLayoutResult> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_layout_result
            WHERE result_id = #{resultId}
            """)
    TLayoutResult selectByIdIgnoreTenant(@Param("resultId") Long resultId);
}
