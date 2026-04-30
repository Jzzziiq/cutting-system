package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TOffcutVO {
    private Long offcutId;
    private Long boardId;
    private Long sourceOrderId;
    private Integer width;
    private Integer length;
    private Integer thickness;
    private String materialType;
    private String brand;
    private String color;
    private Integer status;
    private Integer isEnabled;
    private Date createTime;
    private Date updateTime;
    private String remark;
}
