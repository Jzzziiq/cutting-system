package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private TUserService tUserService;
    @Autowired
    private JwtUtil jwtUtil;
    // 登录
    @RequestMapping("/login")
    public Result login(String username, String password) {
        LoginInfo info = tUserService.login(username, password);
        if (info != null) {
            return Result.success(info);
        }
        return Result.error("用户名或密码错误");
    }

    // 注册
    @RequestMapping("register")
    public Result register(String username, String password){
        boolean f = tUserService.register(username, password);
        if (!f)
            return Result.fail("用户名已存在");
        return Result.success();
    }
    /**
     * 登出功能
     * 后续可以增加redis的时候将当前用户token设置进入黑名单
     */
    @RequestMapping("logout")
    public Result logout(){
        return Result.success();
    }

}
