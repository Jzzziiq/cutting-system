package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.entitys.DTO.AuthRequestDTO;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    @Autowired
    private TUserService tUserService;
    @Autowired
    private JwtUtil jwtUtil;
    // 登录
    @RequestMapping("/login")
    public Result login(@Valid AuthRequestDTO authRequestDTO) {
        LoginInfo info = tUserService.login(authRequestDTO.getUsername(), authRequestDTO.getPassword());
        if (info != null) {
            return Result.success(info);
        }
        return Result.error("用户名或密码错误");
    }

    // 注册
    @RequestMapping("register")
    public Result register(@Valid AuthRequestDTO authRequestDTO){
        boolean f = tUserService.register(authRequestDTO.getUsername(), authRequestDTO.getPassword());
        if (!f)
            return Result.fail("用户名已存在");
        return Result.success();
    }

    /**
     * 注册组织：创建组织 + 创建组织管理员账号，自动登录
     */
    @RequestMapping("/register-org")
    public Result registerOrg(@RequestParam String orgName,
                              @RequestParam String orgCode,
                              @RequestParam String password) {
        if (orgName == null || orgName.isBlank()) {
            return Result.fail("组织名称不能为空");
        }
        if (orgCode == null || orgCode.isBlank()) {
            return Result.fail("组织编码不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        LoginInfo info = tUserService.registerOrg(orgName, orgCode, password);
        if (info == null) {
            return Result.fail("组织编码已存在");
        }
        return Result.success(info);
    }

    /**
     * 注册用户加入组织（需要管理员审批）
     */
    @RequestMapping("/register-user")
    public Result registerUser(@RequestParam String orgCode,
                               @RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(required = false) String realName,
                               @RequestParam(required = false) String phone) {
        if (orgCode == null || orgCode.isBlank()) {
            return Result.fail("组织编码不能为空");
        }
        if (username == null || username.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.fail("密码长度不能少于6位");
        }
        boolean success = tUserService.registerUser(orgCode, username, password, realName, phone);
        if (!success) {
            return Result.fail("组织编码无效或用户名已存在");
        }
        return Result.success("注册成功，请等待管理员审批");
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
