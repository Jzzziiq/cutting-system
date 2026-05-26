package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TAuditLogVO {
    private Long logId;
    private Long userId;
    private String operatorName;
    private String module;
    private String action;
    private String targetClass;
    private String targetMethod;
    private String requestParams;
    private Long durationMs;
    private Integer status;
    private String errorMsg;
    private Date createTime;
}
