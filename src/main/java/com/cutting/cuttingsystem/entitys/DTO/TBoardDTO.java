package com.cutting.cuttingsystem.entitys.DTO;

import lombok.Data;

@Data
public class TBoardDTO {
    private String brand;
    private String materialType;
    private String color;
    // 以下三个有默认值（前端选择：标准板、长门板、自定义）
    private String size;
    private Integer width;
    private Integer length;
    private Integer thickness;

    private String remark;
}
