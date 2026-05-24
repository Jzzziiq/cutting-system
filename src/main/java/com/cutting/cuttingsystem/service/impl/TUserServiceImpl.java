package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TOrganization;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.mapper.TUserMapper;
import com.cutting.cuttingsystem.service.TOrganizationService;
import com.cutting.cuttingsystem.service.TRoleService;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import com.cutting.cuttingsystem.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements TUserService {
    @Autowired
    private TUserMapper tUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TRoleService roleService;

    @Autowired
    private TOrganizationService organizationService;

    @Override
    public LoginInfo login(String username, String password) {
        TUser tUser = tUserMapper.selectByUsername(username);
        if (tUser != null && tUser.getPassword().equals(MD5Util.md5(password))) {
            // Check account status: 1=normal, 2=disabled, 3=pending approval
            if (tUser.getAccountStatus() != null && tUser.getAccountStatus() != 1) {
                return null;
            }
            List<String> roles = roleService.listRoleCodesByUserId(tUser.getUserId());
            List<String> permissions = roleService.listPermissionCodesByUserId(tUser.getUserId());
            LoginInfo info = new LoginInfo();
            info.setUserId(tUser.getUserId());
            info.setUsername(tUser.getUsername());
            info.setRealName(tUser.getRealName());
            info.setToken(jwtUtil.generateToken(tUser, roles));
            info.setRoles(roles);
            info.setPermissions(permissions);
            info.setOrgId(tUser.getOrgId());
            info.setOrgRole(tUser.getOrgRole());
            if (tUser.getOrgId() != null) {
                TOrganization org = organizationService.getById(tUser.getOrgId());
                if (org != null) info.setOrgName(org.getOrgName());
            }
            return info;
        }
        return null;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean register(String username, String password) {
        QueryWrapper<TUser> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Long count = tUserMapper.selectCount(qw);
        if (count == 0) {
            TUser user = new TUser();
            user.setUsername(username);
            user.setPassword(MD5Util.md5(password));
            tUserMapper.insert(user);
            return true;
        }
        log.info("username already exists: {}", username);
        return false;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public LoginInfo registerOrg(String orgName, String orgCode, String password) {
        // Check orgCode uniqueness
        TOrganization existingOrg = organizationService.getOne(
                new QueryWrapper<TOrganization>().eq("org_code", orgCode));
        if (existingOrg != null) {
            return null; // org code already exists
        }

        // Create organization
        TOrganization org = new TOrganization();
        org.setOrgName(orgName);
        org.setOrgCode(orgCode);
        org.setStatus(1);
        organizationService.save(org);

        // Create org_admin user (username = orgCode)
        TUser user = new TUser();
        user.setUsername(orgCode);
        user.setPassword(MD5Util.md5(password));
        user.setOrgId(org.getOrgId());
        user.setOrgRole("org_admin");
        user.setAccountStatus(1);
        user.setRealName(orgName + "管理员");
        tUserMapper.insert(user);

        // Assign org_admin role
        roleService.assignUserRoles(user.getUserId(), List.of(getRoleIds("org_admin")));

        // Auto-login
        List<String> roles = roleService.listRoleCodesByUserId(user.getUserId());
        List<String> permissions = roleService.listPermissionCodesByUserId(user.getUserId());
        LoginInfo info = new LoginInfo();
        info.setUserId(user.getUserId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setToken(jwtUtil.generateToken(user, roles));
        info.setRoles(roles);
        info.setPermissions(permissions);
        info.setOrgId(org.getOrgId());
        info.setOrgRole("org_admin");
        info.setOrgName(org.getOrgName());
        return info;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public boolean registerUser(String orgCode, String username, String password, String realName, String phone) {
        // Validate orgCode
        TOrganization org = organizationService.getOne(
                new QueryWrapper<TOrganization>().eq("org_code", orgCode));
        if (org == null || org.getStatus() != 1) {
            return false; // org not found or disabled
        }

        // Check username uniqueness
        Long count = tUserMapper.selectCount(
                new QueryWrapper<TUser>().eq("username", username));
        if (count > 0) {
            return false; // username already exists
        }

        // Create user with pending approval status
        TUser user = new TUser();
        user.setUsername(username);
        user.setPassword(MD5Util.md5(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setOrgId(org.getOrgId());
        user.setOrgRole("operator"); // default role, admin can change
        user.setAccountStatus(3); // pending approval
        tUserMapper.insert(user);

        // Assign operator role
        roleService.assignUserRoles(user.getUserId(), List.of(getRoleIds("operator")));
        return true;
    }

    private Long getRoleIds(String roleCode) {
        return roleService.getOne(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TRole>()
                        .eq("role_code", roleCode)).getRoleId();
    }
}
