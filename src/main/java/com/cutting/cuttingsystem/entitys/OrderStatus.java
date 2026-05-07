package com.cutting.cuttingsystem.entitys;

import java.util.Map;
import java.util.Set;

/** 订单生命周期状态机 */
public enum OrderStatus {

    PENDING_REVIEW(0, "待审核", Set.of(1, -1)),
    APPROVED(1, "已审核", Set.of(2, -2)),
    CALCULATING(2, "排样中", Set.of(3, -3)),
    LAYOUT_DONE(3, "已排样", Set.of(4, -2)),
    IN_PRODUCTION(4, "生产中", Set.of(5)),
    COMPLETED(5, "已完工", Set.of(6)),
    DELIVERED(6, "已交付", Set.of()),
    REJECTED(-1, "已驳回", Set.of(0)),
    CANCELLED(-2, "已取消", Set.of()),
    LAYOUT_FAILED(-3, "排样失败", Set.of(1));

    private final int code;
    private final String label;
    private final Set<Integer> allowedNext; // 允许转换到的目标状态码

    OrderStatus(int code, String label, Set<Integer> allowedNext) {
        this.code = code;
        this.label = label;
        this.allowedNext = allowedNext;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    public boolean canTransitionTo(int targetCode) {
        return allowedNext.contains(targetCode);
    }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知订单状态码: " + code);
    }

    public static Map<Integer, String> allLabels() {
        Map<Integer, String> map = new java.util.LinkedHashMap<>();
        for (OrderStatus s : values()) {
            map.put(s.code, s.label);
        }
        return map;
    }
}
