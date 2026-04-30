package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_order_item")
public class TOrderItem implements Serializable {
    @TableId
    private Long itemId;

    @TableField(fill = FieldFill.INSERT)
    private Long userId;

    private Long orderId;

    private String partName;

    private String partCode;

    private Long boardId;

    private Long offcutId;

    private Integer width;

    private Integer length;

    private Integer thickness;

    private Integer quantity;

    private String materialName;

    private String color;

    private Integer edgeLeft;

    private Integer edgeRight;

    private Integer edgeFront;

    private Integer edgeBack;

    private Integer edgeTop;

    private Integer edgeBottom;

    private Integer isTexture;

    private Integer allowRotation;

    private String label;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    private String remark;
}
