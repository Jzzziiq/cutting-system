package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_cabinet_order_item")
public class CabinetOrderItem implements Serializable {
    @TableId
    private Long id;
    private Long orderItemId;
    private Long orderId;
    @TableField(fill = FieldFill.INSERT)
    private Long userId;
    private String splitBatchCode;
    private String sourceBoardId;
    private String workpieceCode;
    private String cabinetName;
    private String room;
    private String purpose;
    private String boardType;
    private Integer thickness;
    private String grainDirection;
    private Integer designLength;
    private Integer designWidth;
    private Double positionX;
    private Double positionY;
    private Double positionZ;
    private String edgeBanding;
    private String edgeRole;
    private String holeOperations;
    private String sourceBoardJson;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
