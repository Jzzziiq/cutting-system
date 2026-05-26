package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.VO.TAuditLogVO;
import com.cutting.cuttingsystem.mapper.TAuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequirePermission("audit:read")
public class AuditLogController {

    @Autowired
    private TAuditLogMapper auditLogMapper;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String module,
                       @RequestParam(required = false) Long userId,
                       @RequestParam(required = false) Integer status) {
        Page<TAuditLogVO> page = new Page<>(pageNum, pageSize);
        return Result.success(auditLogMapper.selectLogPage(page, module, userId, status));
    }
}
