package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TPermission;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface TPermissionMapper extends BaseMapper<TPermission> {

    @Select("SELECT DISTINCT p.* FROM t_permission p " +
            "INNER JOIN t_role_permission rp ON p.perm_id = rp.perm_id " +
            "INNER JOIN t_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<TPermission> selectPermissionsByUserId(Long userId);

    @Select("<script>" +
            "SELECT DISTINCT p.perm_code FROM t_permission p " +
            "INNER JOIN t_role_permission rp ON p.perm_id = rp.perm_id " +
            "INNER JOIN t_role r ON rp.role_id = r.role_id " +
            "WHERE r.role_code IN " +
            "<foreach item='code' collection='roleCodes' open='(' separator=',' close=')'>#{code}</foreach>" +
            "</script>")
    List<String> selectPermCodesByRoleCodes(List<String> roleCodes);
}
