package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_production_task")
public class TProductionTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    @TableField(fill = FieldFill.INSERT)
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private Long orgId;

    private Long orderId;
    private Long layoutResultId;
    private String taskName;
    private Long assigneeId;
    private String assigneeName;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private Integer status;
    private Date startTime;
    private Date completeTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
