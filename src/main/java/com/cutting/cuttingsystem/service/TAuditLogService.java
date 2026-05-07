package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.TAuditLog;

public interface TAuditLogService extends IService<TAuditLog> {

    void asyncSave(TAuditLog logEntry);
}
