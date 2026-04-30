package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_order")
public class TOrder implements Serializable {
    @TableId
    private Long orderId;

    @TableField(fill = FieldFill.INSERT)
    private Long userId;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String customerAddress;

    private String processName;

    private Integer orderStatus;

    private String rawMaterialJson;

    private String remnantJson;

    private String configJson;

    private Long layoutResultId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    private Date finishTime;

    private String remark;
}
