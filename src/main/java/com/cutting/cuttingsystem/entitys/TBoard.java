package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 板材表
 * @TableName t_board
 */
@Data
@TableName("t_board")
public class TBoard implements Serializable {

    /**
     * 板材唯一 ID（原材序号）
     */
    @TableId
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
     * 使用频次
     */
    private Integer useCount;
    
    /**
     * 最后调用时间
     */
    private Date lastUseTime;
    
    /**
     * 是否启用：1=启用，0=禁用
     */
    private Integer isEnabled;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 备注
     */
    private String remark;

}
