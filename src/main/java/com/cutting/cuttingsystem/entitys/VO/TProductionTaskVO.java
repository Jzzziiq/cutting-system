package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TProductionTaskVO {
    private Long taskId;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private Long layoutResultId;
    private String taskName;
    private Long assigneeId;
    private String assigneeName;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer status;
    private String statusLabel;
    private Date startTime;
    private Date completeTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
