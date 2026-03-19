package com.cutting.cuttingsystem.util;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserIdHandler implements TenantLineHandler {
    @Override
    public Expression getTenantId() {
        Long currentUserId = UserContext.getCurrentUserId();
        return new LongValue(currentUserId);
    }

    @Override
    public String getTenantIdColumn() {
        return "user_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 不需要多租户的表（如系统表、用户表等）
        return "t_user".equalsIgnoreCase(tableName);
    }

//    @Override
//    public boolean ignoreInsert(List<Column> columns, String tenantIdColumn) {
//        return TenantLineHandler.super.ignoreInsert(columns, tenantIdColumn);
//    }
}
