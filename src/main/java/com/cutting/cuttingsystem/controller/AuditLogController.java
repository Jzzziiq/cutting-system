package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TAuditLog;
import com.cutting.cuttingsystem.service.TAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequirePermission("audit:read")
public class AuditLogController {

    @Autowired
    private TAuditLogService auditLogService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String module,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) Integer status) {
        QueryWrapper<TAuditLog> qw = new QueryWrapper<>();
        if (StringUtils.hasText(module)) {
            qw.eq("module", module);
        }
        if (StringUtils.hasText(username)) {
            qw.eq("username", username);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("create_time");
        return Result.success(auditLogService.page(new Page<>(pageNum, pageSize), qw));
    }
}
