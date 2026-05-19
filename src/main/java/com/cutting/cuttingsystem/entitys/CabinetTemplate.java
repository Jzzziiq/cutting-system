package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_cabinet_template")
public class CabinetTemplate implements Serializable {
    @TableId
    private Long id;
    private String name;
    private String category;
    private String thumbnail;
    private String cabinetJson;
    private Integer isOfficial;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
