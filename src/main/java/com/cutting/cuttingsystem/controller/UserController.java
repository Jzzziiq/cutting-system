package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.AssignRoleDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TRole;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.entitys.VO.UserVO;
import com.cutting.cuttingsystem.service.TRoleService;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.MD5Util;
import com.cutting.cuttingsystem.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Validated
@RequirePermission({"user:manage", "account:manage"})
public class UserController {

    @Autowired
    private TUserService userService;

    @Autowired
    private TRoleService roleService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<TUser> qw = new QueryWrapper<>();
        // always hide super admin from list
        qw.ne("username", "admin");
        // org_admin sees only their org users; admin sees all
        Long orgId = UserContext.getCurrentOrgId();
        List<String> roles = UserContext.getRoles();
        if (!roles.contains("admin") && orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("create_time");
        Page<TUser> page = userService.page(new Page<>(pageNum, pageSize), qw);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        Page<UserVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    /**
     * 获取待审批用户列表
     */
    @GetMapping("/pending")
    public Result listPending(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<TUser> qw = new QueryWrapper<>();
        qw.ne("username", "admin");
        qw.eq("account_status", 3);
        Long orgId = UserContext.getCurrentOrgId();
        List<String> roles = UserContext.getRoles();
        if (!roles.contains("admin") && orgId != null) {
            qw.eq("org_id", orgId);
        }
        qw.orderByDesc("create_time");
        Page<TUser> page = userService.page(new Page<>(pageNum, pageSize), qw);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        Page<UserVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        TUser user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        return Result.success(toVO(user));
    }

    @PutMapping("/{id}/status")
    @AuditLog(module = "用户管理", action = "启禁")
    public Result updateStatus(@PathVariable Long id, @RequestParam Integer accountStatus) {
        TUser user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        user.setAccountStatus(accountStatus);
        userService.updateById(user);
        return Result.success();
    }

    @PutMapping("/roles")
    @AuditLog(module = "用户管理", action = "分配角色")
    public Result assignRoles(@Valid @RequestBody AssignRoleDTO dto) {
        if (!userService.exists(new QueryWrapper<TUser>().eq("user_id", dto.getUserId()))) {
            return Result.error("用户不存在");
        }
        roleService.assignUserRoles(dto.getUserId(), dto.getRoleIds());
        return Result.success();
    }

    /**
     * 分配组织内角色（org_admin/operator/viewer）
     */
    @PutMapping("/{id}/org-role")
    @AuditLog(module = "用户管理", action = "分配组织角色")
    public Result assignOrgRole(@PathVariable Long id, @RequestParam String orgRole) {
        if (!List.of("org_admin", "operator", "viewer").contains(orgRole)) {
            return Result.error("无效的组织角色");
        }
        TUser user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        user.setOrgRole(orgRole);
        userService.updateById(user);
        return Result.success();
    }

    @GetMapping("/roles")
    @RequirePermission({}) // 登录即可访问，用于获取角色列表
    public Result listRoles() {
        List<TRole> roles = roleService.listAllRoles();
        return Result.success(roles);
    }

    /**
     * 组织管理员在本组织内创建操作员/生产员账号
     */
    @PostMapping("/create-in-org")
    @RequirePermission("user:manage")
    @AuditLog(module = "用户管理", action = "创建组织成员")
    public Result createInOrg(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String realName = (String) body.get("realName");
        String phone = (String) body.get("phone");
        Integer roleType = body.get("roleType") != null ? ((Number) body.get("roleType")).intValue() : 2;

        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }
        Long orgId = UserContext.getCurrentOrgId();
        if (orgId == null) {
            return Result.error("当前用户不属于任何组织");
        }

        // check username uniqueness
        TUser existing = userService.getOne(new QueryWrapper<TUser>().eq("username", username));
        if (existing != null) {
            return Result.error("用户名已存在");
        }

        TUser user = new TUser();
        user.setUsername(username);
        user.setPassword(MD5Util.md5(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRoleType(roleType);
        user.setAccountStatus(1); // auto-approved
        user.setOrgId(orgId);
        user.setOrgRole(roleType == 2 ? "operator" : "viewer");
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        userService.save(user);

        // assign role
        String roleCode = roleType == 2 ? "operator" : "viewer";
        List<TRole> allRoles = roleService.listAllRoles();
        allRoles.stream().filter(r -> r.getRoleCode().equals(roleCode)).findFirst().ifPresent(r -> {
            roleService.assignUserRoles(user.getUserId(), List.of(r.getRoleId()));
        });

        return Result.success(toVO(user));
    }

    /**
     * 当前用户修改自己的密码
     */
    @PutMapping("/me/password")
    @RequirePermission({})
    @AuditLog(module = "用户管理", action = "修改密码")
    public Result changeMyPassword(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return Result.error("旧密码和新密码不能为空");
        }
        Long userId = UserContext.getCurrentUserId();
        TUser user = userService.getById(userId);
        if (user == null) return Result.error("用户不存在");
        if (!user.getPassword().equals(MD5Util.md5(oldPassword))) {
            return Result.error("旧密码不正确");
        }
        user.setPassword(MD5Util.md5(newPassword));
        user.setUpdateTime(new Date());
        userService.updateById(user);
        return Result.success();
    }

    private UserVO toVO(TUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roleService.listRoleCodesByUserId(user.getUserId()));
        vo.setPermissions(roleService.listPermissionCodesByUserId(user.getUserId()));
        return vo;
    }
}
