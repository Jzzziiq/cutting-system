package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TUser;

/**
* @author JZQ
* @description 针对表【t_user(存储系统所有用户账号信息，区分管理员与生产人员双角色，实现账号权限管控与注册审批流程)】的数据库操作Service
* @createDate 2026-03-14 16:08:00
*/
public interface TUserService extends IService<TUser> {

    LoginInfo login(String username, String password);

    /**
     * 查询username是否已经存在，不存在则使用该用户名密码创建账号
     * 密码需要使用md5进行加密
     * 一查一增
     */
    boolean register(String username, String password);
}
