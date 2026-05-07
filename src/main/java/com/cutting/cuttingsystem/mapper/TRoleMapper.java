package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TRole;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface TRoleMapper extends BaseMapper<TRole> {

    @Select("SELECT r.* FROM t_role r INNER JOIN t_user_role ur ON r.role_id = ur.role_id WHERE ur.user_id = #{userId}")
    List<TRole> selectRolesByUserId(Long userId);
}
