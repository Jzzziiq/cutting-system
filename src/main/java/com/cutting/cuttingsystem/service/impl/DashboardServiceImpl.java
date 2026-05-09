package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cutting.cuttingsystem.entitys.OrderStatus;
import com.cutting.cuttingsystem.mapper.*;
import com.cutting.cuttingsystem.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private TOrderMapper orderMapper;
    @Autowired
    private TProductionTaskMapper productionTaskMapper;
    @Autowired
    private TLayoutResultMapper layoutResultMapper;
    @Autowired
    private TCustomerMapper customerMapper;
    @Autowired
    private TBoardMapper boardMapper;

    @Override
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();

        long orderTotal = orderMapper.selectCount(null);
        long activeOrders = orderMapper.selectCount(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TOrder>().in("order_status", Arrays.asList(
                        OrderStatus.APPROVED.getCode(),
                        OrderStatus.CALCULATING.getCode(),
                        OrderStatus.LAYOUT_DONE.getCode(),
                        OrderStatus.IN_PRODUCTION.getCode())));

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        long orderToday = orderMapper.selectCount(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TOrder>().apply("DATE(create_time) = {0}", today));
        String monthStart = LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        long orderThisMonth = orderMapper.selectCount(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TOrder>().apply("DATE(create_time) >= {0}", monthStart));

        long taskPending = productionTaskMapper.selectCount(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TProductionTask>().eq("status", 0));
        long taskInProgress = productionTaskMapper.selectCount(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TProductionTask>().eq("status", 1));
        long taskCompletedThisWeek = productionTaskMapper.selectCount(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TProductionTask>().eq("status", 2)
                        .apply("DATE(complete_time) >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)"));

        long customerCount = customerMapper.selectCount(null);
        long boardCount = boardMapper.selectCount(null);

        Map<String, Object> avgRateRow = layoutResultMapper.selectMaps(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TLayoutResult>().select("IFNULL(AVG(usage_rate),0) as avgRate")).stream()
                .findFirst().orElse(Map.of("avgRate", 0));

        result.put("orderTotal", orderTotal);
        result.put("activeOrders", activeOrders);
        result.put("orderToday", orderToday);
        result.put("orderThisMonth", orderThisMonth);
        result.put("taskPending", taskPending);
        result.put("taskInProgress", taskInProgress);
        result.put("taskCompletedThisWeek", taskCompletedThisWeek);
        result.put("customerCount", customerCount);
        result.put("boardCount", boardCount);
        result.put("avgUtilization", round(avgRateRow.get("avgRate")));
        return result;
    }

    @Override
    public Map<String, Object> orderTrend(int days) {
        List<Map<String, Object>> rows = orderMapper.selectMaps(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TOrder>()
                        .select("DATE(create_time) as date", "COUNT(*) as count")
                        .apply("create_time >= DATE_SUB(CURDATE(), INTERVAL {0} DAY)", days)
                        .groupBy("DATE(create_time)")
                        .orderByAsc("DATE(create_time)"));

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        LocalDate cursor = LocalDate.now().minusDays(days - 1);
        Map<String, Long> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            rowMap.put(String.valueOf(row.get("date")), (Long) row.get("count"));
        }
        for (int i = 0; i < days; i++) {
            String d = cursor.format(DateTimeFormatter.ISO_LOCAL_DATE);
            labels.add(d.substring(5));
            data.add(rowMap.getOrDefault(d, 0L));
            cursor = cursor.plusDays(1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> orderStatusDist() {
        List<Map<String, Object>> rows = orderMapper.selectMaps(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TOrder>()
                        .select("order_status", "COUNT(*) as count")
                        .groupBy("order_status"));

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int code = ((Number) row.get("order_status")).intValue();
            labels.add(OrderStatus.fromCode(code).getLabel());
            data.add((Long) row.get("count"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    @Override
    public Map<String, Object> utilizationTrend(int days) {
        List<Map<String, Object>> rows = layoutResultMapper.selectMaps(
                new QueryWrapper<com.cutting.cuttingsystem.entitys.TLayoutResult>()
                        .select("DATE(create_time) as date", "AVG(usage_rate) as avgRate")
                        .apply("create_time >= DATE_SUB(CURDATE(), INTERVAL {0} DAY)", days)
                        .groupBy("DATE(create_time)")
                        .orderByAsc("DATE(create_time)"));

        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        LocalDate cursor = LocalDate.now().minusDays(days - 1);
        Map<String, BigDecimal> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            BigDecimal avg = row.get("avgRate") != null
                    ? new BigDecimal(row.get("avgRate").toString()) : BigDecimal.ZERO;
            rowMap.put(String.valueOf(row.get("date")), avg);
        }
        for (int i = 0; i < days; i++) {
            String d = cursor.format(DateTimeFormatter.ISO_LOCAL_DATE);
            labels.add(d.substring(5));
            data.add(round(rowMap.getOrDefault(d, BigDecimal.ZERO)));
            cursor = cursor.plusDays(1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }

    private BigDecimal round(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString()).setScale(3, RoundingMode.HALF_UP);
    }
}
