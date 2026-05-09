package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@Validated
@RequirePermission({"order:read"})
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public Result summary() {
        return Result.success(dashboardService.summary());
    }

    @GetMapping("/order-trend")
    public Result orderTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.orderTrend(Math.min(days, 365)));
    }

    @GetMapping("/order-status-dist")
    public Result orderStatusDist() {
        return Result.success(dashboardService.orderStatusDist());
    }

    @GetMapping("/utilization-trend")
    public Result utilizationTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.success(dashboardService.utilizationTrend(Math.min(days, 365)));
    }
}
