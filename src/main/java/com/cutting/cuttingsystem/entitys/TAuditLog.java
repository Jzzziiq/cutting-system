package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_audit_log")
public class TAuditLog implements Serializable {
    @TableId
    private Long logId;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetClass;
    private String targetMethod;
    private String requestParams;
    private String ipAddress;
    private Long durationMs;
    private Integer status;
    private String errorMsg;
    private Date createTime;
}
