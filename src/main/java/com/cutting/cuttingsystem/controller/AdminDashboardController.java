package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TOrganization;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TOrganizationService;
import com.cutting.cuttingsystem.service.TUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理员仪表盘（跨组织统计）
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequirePermission("account:manage")
public class AdminDashboardController {

    @Autowired
    private TOrganizationService organizationService;

    @Autowired
    private TUserService userService;

    @GetMapping
    public Result summary() {
        long orgCount = organizationService.count();
        long activeOrgCount = organizationService.count(
                new QueryWrapper<TOrganization>().eq("status", 1));
        long userCount = userService.count(
                new QueryWrapper<TUser>().ne("username", "admin"));
        List<TOrganization> recentOrgs = organizationService.list(
                new QueryWrapper<TOrganization>().orderByDesc("create_time").last("LIMIT 5"));

        Map<String, Object> data = new HashMap<>();
        data.put("orgCount", orgCount);
        data.put("activeOrgCount", activeOrgCount);
        data.put("userCount", userCount);
        data.put("recentOrgs", recentOrgs);
        return Result.success(data);
    }
}
