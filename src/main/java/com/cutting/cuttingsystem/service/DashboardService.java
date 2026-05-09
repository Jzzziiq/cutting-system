package com.cutting.cuttingsystem.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> summary();
    Map<String, Object> orderTrend(int days);
    Map<String, Object> orderStatusDist();
    Map<String, Object> utilizationTrend(int days);
}
