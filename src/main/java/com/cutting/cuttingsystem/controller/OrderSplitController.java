package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.SplitConfirmRequest;
import com.cutting.cuttingsystem.entitys.DTO.SplitExecuteRequest;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.VO.SplitConfirmResultVO;
import com.cutting.cuttingsystem.entitys.VO.SplitItemVO;
import com.cutting.cuttingsystem.service.OrderSplitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order-split")
@Validated
public class OrderSplitController {
    @Autowired
    private OrderSplitService orderSplitService;

    @PostMapping("/execute")
    @RequirePermission("order:write")
    public Result execute(@RequestBody @Valid SplitExecuteRequest request) {
        try {
            List<SplitItemVO> items = orderSplitService.execute(request);
            return Result.success(items);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/confirm")
    @RequirePermission("order:write")
    @AuditLog(module = "柜体拆单", action = "确认拆单")
    public Result confirm(@RequestBody @Valid SplitConfirmRequest request) {
        try {
            SplitConfirmResultVO result = orderSplitService.confirm(request);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
