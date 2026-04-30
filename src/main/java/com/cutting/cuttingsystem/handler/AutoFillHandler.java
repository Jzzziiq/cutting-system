package com.cutting.cuttingsystem.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cutting.cuttingsystem.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class AutoFillHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("start insert auto fill");
        this.strictInsertFill(metaObject, "userId", Long.class, UserContext.getCurrentUserId());
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("start update auto fill");
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
