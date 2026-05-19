package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.CabinetTemplate;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.service.CabinetTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cabinet-templates")
@Validated
public class CabinetTemplateController {
    @Autowired
    private CabinetTemplateService cabinetTemplateService;

    @GetMapping
    @RequirePermission("order:read")
    public Result pageQuery(@Valid QueryDTO query,
                            @RequestParam(required = false) String category) {
        IPage<CabinetTemplate> page = cabinetTemplateService.pageQuery(query, category);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @RequirePermission("order:read")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        CabinetTemplate template = cabinetTemplateService.getTemplateById(id);
        if (template == null) return Result.error("模板不存在或无权限访问");
        return Result.success(template);
    }

    @PostMapping
    @RequirePermission("order:write")
    @AuditLog(module = "柜体模板", action = "保存模板")
    public Result save(@RequestBody @Valid CabinetTemplate template) {
        return Result.success(cabinetTemplateService.createTemplate(template));
    }

    @PutMapping("/{id}")
    @RequirePermission("order:write")
    @AuditLog(module = "柜体模板", action = "编辑模板")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid CabinetTemplate template) {
        try {
            return Result.success(cabinetTemplateService.updateTemplate(id, template));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("order:write")
    @AuditLog(module = "柜体模板", action = "删除模板")
    public Result delete(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        try {
            cabinetTemplateService.deleteTemplate(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
