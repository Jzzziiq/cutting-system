package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

@Data
public class TBoardVO {
    private Long boardId;
    private String brand;
    private String materialType;
    private String color;
    private String sizeType;
    private Integer width;
    private Integer length;
    private Integer thickness;
    private Integer isEnabled;
    private String remark;
}
