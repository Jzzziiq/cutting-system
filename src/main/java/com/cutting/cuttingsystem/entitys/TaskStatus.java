package com.cutting.cuttingsystem.entitys;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum TaskStatus {
    PENDING(0, "待生产", Set.of(1)),
    IN_PROGRESS(1, "生产中", Set.of(2)),
    COMPLETED(2, "已完成", Set.of());

    private final int code;
    private final String label;
    private final Set<Integer> allowedNext;

    private static final Map<Integer, TaskStatus> INDEX = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(TaskStatus::getCode, Function.identity()));

    public static TaskStatus fromCode(int code) {
        TaskStatus s = INDEX.get(code);
        if (s == null) throw new IllegalArgumentException("无效的任务状态码: " + code);
        return s;
    }

    public boolean canTransitionTo(int targetCode) {
        return allowedNext.contains(targetCode);
    }

    public static Map<Integer, String> allLabels() {
        return INDEX.values().stream().collect(Collectors.toMap(TaskStatus::getCode, TaskStatus::getLabel));
    }
}
