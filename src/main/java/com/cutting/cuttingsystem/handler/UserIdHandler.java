package com.cutting.cuttingsystem.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.cutting.cuttingsystem.util.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserIdHandler implements TenantLineHandler {
    private static final List<String> IGNORE_TABLES = List.of(
        "t_user", "t_role", "t_permission", "t_user_role", "t_role_permission"
    );

    @Override
    public Expression getTenantId() {
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) {
            return new LongValue(0L);
        }
        return new LongValue(currentUserId);
    }

    @Override
    public String getTenantIdColumn() {
        return "user_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }

//    @Override
//    public boolean ignoreInsert(List<Column> columns, String tenantIdColumn) {
//        return TenantLineHandler.super.ignoreInsert(columns, tenantIdColumn);
//    }
}
