package com.cutting.cuttingsystem.aop;

import com.cutting.cuttingsystem.entitys.TAuditLog;
import com.cutting.cuttingsystem.mapper.TAuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 独立的审计日志异步写入组件。
 * 不能放在 MyBatis-Plus ServiceImpl 子类中，否则 @Async 与 ServiceImpl 的 CGLIB 代理冲突导致失效。
 */
@Slf4j
@Component
public class AuditLogWriter {

    @Autowired
    private TAuditLogMapper auditLogMapper;

    @Async("auditLogExecutor")
    public void asyncSave(TAuditLog logEntry) {
        try {
            auditLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("审计日志写入失败", e);
        }
    }
}
