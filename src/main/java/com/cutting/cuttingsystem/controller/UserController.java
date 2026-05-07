package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.AssignRoleDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TRole;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.entitys.VO.UserVO;
import com.cutting.cuttingsystem.service.TRoleService;
import com.cutting.cuttingsystem.service.TUserService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@Validated
@RequirePermission("user:manage")
public class UserController {

    @Autowired
    private TUserService userService;

    @Autowired
    private TRoleService roleService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<TUser> page = userService.page(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<TUser>().orderByDesc("create_time"));
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
    public Result updateStatus(@PathVariable Long id, @RequestParam Integer accountStatus) {
        TUser user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        user.setAccountStatus(accountStatus);
        userService.updateById(user);
        return Result.success();
    }

    @PutMapping("/roles")
    public Result assignRoles(@Valid @RequestBody AssignRoleDTO dto) {
        if (!userService.exists(new QueryWrapper<TUser>().eq("user_id", dto.getUserId()))) {
            return Result.error("用户不存在");
        }
        roleService.assignUserRoles(dto.getUserId(), dto.getRoleIds());
        return Result.success();
    }

    @GetMapping("/roles")
    @RequirePermission({}) // 登录即可访问，用于获取角色列表
    public Result listRoles() {
        List<TRole> roles = roleService.listAllRoles();
        return Result.success(roles);
    }

    private UserVO toVO(TUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoles(roleService.listRoleCodesByUserId(user.getUserId()));
        vo.setPermissions(roleService.listPermissionCodesByUserId(user.getUserId()));
        return vo;
    }
}
