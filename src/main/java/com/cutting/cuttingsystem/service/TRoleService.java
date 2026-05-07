package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TPermission;
import com.cutting.cuttingsystem.entitys.TRole;

import java.util.List;

public interface TRoleService extends IService<TRole> {

    List<TRole> listRolesByUserId(Long userId);

    List<String> listRoleCodesByUserId(Long userId);

    List<TPermission> listPermissionsByUserId(Long userId);

    List<String> listPermissionCodesByUserId(Long userId);

    List<TRole> listAllRoles();

    void assignUserRoles(Long userId, List<Long> roleIds);
}
