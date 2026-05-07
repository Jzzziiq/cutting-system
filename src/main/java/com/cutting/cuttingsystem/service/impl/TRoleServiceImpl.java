package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TPermission;
import com.cutting.cuttingsystem.entitys.TRole;
import com.cutting.cuttingsystem.entitys.TUserRole;
import com.cutting.cuttingsystem.mapper.TPermissionMapper;
import com.cutting.cuttingsystem.mapper.TRoleMapper;
import com.cutting.cuttingsystem.mapper.TUserRoleMapper;
import com.cutting.cuttingsystem.service.TRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TRoleServiceImpl extends ServiceImpl<TRoleMapper, TRole> implements TRoleService {

    @Autowired
    private TRoleMapper roleMapper;

    @Autowired
    private TPermissionMapper permissionMapper;

    @Autowired
    private TUserRoleMapper userRoleMapper;

    @Override
    public List<TRole> listRolesByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<String> listRoleCodesByUserId(Long userId) {
        List<TRole> roles = listRolesByUserId(userId);
        return roles.stream().map(TRole::getRoleCode).toList();
    }

    @Override
    public List<TPermission> listPermissionsByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        return permissionMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public List<String> listPermissionCodesByUserId(Long userId) {
        List<TPermission> perms = listPermissionsByUserId(userId);
        return perms.stream().map(TPermission::getPermCode).toList();
    }

    @Override
    public List<TRole> listAllRoles() {
        return list();
    }

    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new QueryWrapper<TUserRole>().eq("user_id", userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            List<TUserRole> list = new ArrayList<>();
            for (Long roleId : roleIds) {
                TUserRole ur = new TUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.insert(list);
        }
    }
}
