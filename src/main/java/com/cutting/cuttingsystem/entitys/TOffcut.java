package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_offcut")
public class TOffcut implements Serializable {
    @TableId
    private Long offcutId;

    @TableField(fill = FieldFill.INSERT)
    private Long userId;

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

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    private String remark;
}
