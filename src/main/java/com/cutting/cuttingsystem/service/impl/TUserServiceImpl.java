package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.mapper.TUserMapper;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import com.cutting.cuttingsystem.util.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements TUserService {
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
}
