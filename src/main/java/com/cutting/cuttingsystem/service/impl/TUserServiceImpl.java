package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.mapper.TUserMapper;
import com.cutting.cuttingsystem.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author JZQ
 * @description 针对表【t_user(存储系统所有用户账号信息，区分管理员与生产人员双角色，实现账号权限管控与注册审批流程)】的数据库操作Service实现
 * @createDate 2026-03-14 16:08:00
 */
@Service
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser>
        implements TUserService {
    @Autowired
    private TUserMapper tUserMapper;

    @Override
    public boolean login(String username, String password) {
        TUser tUser = tUserMapper.selectByUsername(username);
        if (tUser != null) {
            String encryptedPassword = MD5Util.md5(password);
            return tUser.getPassword().equals(encryptedPassword);
        }
        return false;
    }
}




