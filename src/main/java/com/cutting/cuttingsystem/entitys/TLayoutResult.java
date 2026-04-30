package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_layout_result")
public class TLayoutResult implements Serializable {
    @TableId
    private Long resultId;

    private Long orderId;

    @TableField(fill = FieldFill.INSERT)
    private Long userId;

    private BigDecimal usageRate;

    private BigDecimal totalArea;

    private Integer containerCount;

    private String resultJson;

    private String imagePath;

    private String ncFilePath;

    private String labelFilePath;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
