package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TProductionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TProductionTaskMapper extends BaseMapper<TProductionTask> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_production_task
            WHERE task_id = #{taskId}
            """)
    TProductionTask selectByIdIgnoreTenant(@Param("taskId") Long taskId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_production_task
            WHERE order_id = #{orderId}
            ORDER BY create_time DESC, task_id DESC
            LIMIT 1
            """)
    TProductionTask selectLatestByOrderIdIgnoreTenant(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_production_task
            WHERE order_id = #{orderId}
            ORDER BY create_time DESC, task_id DESC
            """)
    List<TProductionTask> selectByOrderIdIgnoreTenant(@Param("orderId") Long orderId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_production_task
            WHERE assignee_id = #{assigneeId}
            ORDER BY create_time DESC, task_id DESC
            """)
    List<TProductionTask> selectByAssigneeIdIgnoreTenant(@Param("assigneeId") Long assigneeId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_production_task
            WHERE task_id = #{taskId}
              AND assignee_id = #{assigneeId}
            """)
    TProductionTask selectAssignedByIdIgnoreTenant(@Param("taskId") Long taskId,
                                                   @Param("assigneeId") Long assigneeId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE t_production_task
            SET assignee_id = #{assigneeId},
                assignee_name = #{assigneeName},
                update_time = NOW()
            WHERE task_id = #{taskId}
            """)
    int updateAssignmentIgnoreTenant(@Param("taskId") Long taskId,
                                     @Param("assigneeId") Long assigneeId,
                                     @Param("assigneeName") String assigneeName);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM t_production_task
            ORDER BY create_time DESC
            """)
    List<TProductionTask> selectAllIgnoreTenant();

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            DELETE FROM t_production_task
            WHERE task_id = #{taskId}
            """)
    int deleteByIdIgnoreTenant(@Param("taskId") Long taskId);
}
