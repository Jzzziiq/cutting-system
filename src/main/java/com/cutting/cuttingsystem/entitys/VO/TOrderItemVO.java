package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TOrderItemVO {
    private Long itemId;
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
    private Date createTime;
    private Date updateTime;
    private String remark;
}
