package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TAuditLog;
import com.cutting.cuttingsystem.mapper.TAuditLogMapper;
import com.cutting.cuttingsystem.service.TAuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @deprecated 异步写入已迁移至 {@link com.cutting.cuttingsystem.aop.AuditLogWriter}，
 *             因 @Async 与 MyBatis-Plus ServiceImpl 的 CGLIB 代理冲突。
 *             本类保留仅用于可能的同步操作接口实现。
 */
@Slf4j
@Service
public class TAuditLogServiceImpl extends ServiceImpl<TAuditLogMapper, TAuditLog> implements TAuditLogService {

    @Override
    public void asyncSave(TAuditLog logEntry) {
        try {
            save(logEntry);
        } catch (Exception e) {
            log.error("审计日志写入失败", e);
        }
    }
}
