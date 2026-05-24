package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TOrganization;
import com.cutting.cuttingsystem.service.TOrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织管理API（仅系统管理员可访问）
 */
@RestController
@RequestMapping("/organizations")
@Validated
@RequirePermission("account:manage")
public class OrganizationController {

    @Autowired
    private TOrganizationService organizationService;

    /**
     * 列出所有组织
     */
    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<TOrganization> page = organizationService.page(
                new Page<>(pageNum, pageSize),
                new QueryWrapper<TOrganization>().orderByDesc("create_time"));
        return Result.success(page);
    }

    /**
     * 组织详情
     */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        TOrganization org = organizationService.getById(id);
        if (org == null) return Result.error("组织不存在");
        return Result.success(org);
    }

    /**
     * 创建组织
     */
    @PostMapping
    @AuditLog(module = "组织管理", action = "创建")
    public Result create(@RequestBody TOrganization org) {
        // Check org_code uniqueness
        TOrganization existing = organizationService.getOne(
                new QueryWrapper<TOrganization>().eq("org_code", org.getOrgCode()));
        if (existing != null) {
            return Result.fail("组织编码已存在");
        }
        org.setStatus(1);
        organizationService.save(org);
        return Result.success(org);
    }

    /**
     * 启禁组织
     */
    @PutMapping("/{id}/status")
    @AuditLog(module = "组织管理", action = "启禁")
    public Result updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        TOrganization org = organizationService.getById(id);
        if (org == null) return Result.error("组织不存在");
        org.setStatus(status);
        organizationService.updateById(org);
        return Result.success();
    }
}
