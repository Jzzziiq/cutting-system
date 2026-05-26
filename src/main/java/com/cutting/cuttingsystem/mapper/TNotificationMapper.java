package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

@Mapper
public interface TNotificationMapper extends BaseMapper<TNotification> {

    @Update("UPDATE t_notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    @InterceptorIgnore(tenantLine = "true")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE t_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    @InterceptorIgnore(tenantLine = "true")
    int markAllRead(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_notification WHERE user_id = #{userId} AND is_read = 0")
    @InterceptorIgnore(tenantLine = "true")
    int countUnread(@Param("userId") Long userId);
}
