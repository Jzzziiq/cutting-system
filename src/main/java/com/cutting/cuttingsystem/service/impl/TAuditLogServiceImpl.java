package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TAuditLog;
import com.cutting.cuttingsystem.mapper.TAuditLogMapper;
import com.cutting.cuttingsystem.service.TAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TAuditLogServiceImpl extends ServiceImpl<TAuditLogMapper, TAuditLog> implements TAuditLogService {

    @Override
    @Async
    public void asyncSave(TAuditLog logEntry) {
        try {
            save(logEntry);
        } catch (Exception e) {
            log.error("审计日志写入失败", e);
        }
    }
}
