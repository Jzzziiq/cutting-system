package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TUser;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
* @author JZQ
* @description 针对表【t_user(存储系统所有用户账号信息，区分管理员与生产人员双角色，实现账号权限管控与注册审批流程)】的数据库操作Mapper
* @createDate 2026-03-14 16:08:00
* @Entity com.cutting.cuttingsystem.entitys.User
*/
@Component
public interface TUserMapper extends BaseMapper<TUser> {

    @Select("select * from t_user where username = #{username}")
    TUser selectByUsername(String username);
}




