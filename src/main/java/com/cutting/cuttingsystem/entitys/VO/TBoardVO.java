package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

@Data
public class TBoardVO {

    /**
     * 板材唯一 ID
     */
    private Long boardId;

    /**
     * 板材品牌
     */
    private String brand;

    /**
     * 板材材质
     */
    private String materialType;

    /**
     * 板材颜色
     */
    private String color;

    /**
     * 板材尺寸类型
     */
    private String sizeType;

    /**
     * 板材宽度 (mm)
     */
    private Integer width;

    /**
     * 板材长度 (mm)
     */
    private Integer length;

    /**
     * 板材厚度 (mm)
     */
    private Integer thickness;


    /**
     * 是否启用：1=启用，0=禁用
     */
    private Integer isEnabled;


    /**
     * 备注
     */
    private String remark;
}
