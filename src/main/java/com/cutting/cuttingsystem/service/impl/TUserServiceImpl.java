package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.mapper.TUserMapper;
import com.cutting.cuttingsystem.util.JwtUtil;
import com.cutting.cuttingsystem.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author JZQ
 * @description 针对表【t_user(存储系统所有用户账号信息，区分管理员与生产人员双角色，实现账号权限管控与注册审批流程)】的数据库操作Service实现
 * @createDate 2026-03-14 16:08:00
 */
@Slf4j
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser>
        implements TUserService {
    @Autowired
    private TUserMapper tUserMapper;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginInfo login(String username, String password) {
        TUser tUser = tUserMapper.selectByUsername(username);
        if (tUser != null && tUser.getPassword().equals(MD5Util.md5(password))) {
            return new LoginInfo(tUser.getUserId(), tUser.getUsername(), tUser.getRealName(), jwtUtil.generateToken(tUser));
        }
        return null;
    }


    @Override
    public boolean register(String username, String password) {
        // 构建条件查询器并查询是否存在相同用户名
        QueryWrapper<TUser> qw = new QueryWrapper<>();
        qw.eq("username", username);
        Long cnt = tUserMapper.selectCount(qw);
        // 判断是否创建账号(可以去全局异常处理)
        if (cnt == 0){
            TUser user = new TUser();
            user.setUsername(username);
            user.setPassword(MD5Util.md5(password));
            tUserMapper.insert(user);
            return true;
        }
        log.info("用户名重复");
        return false;

    }
}




