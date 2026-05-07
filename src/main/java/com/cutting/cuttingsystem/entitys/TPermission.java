package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_permission")
public class TPermission implements Serializable {
    @TableId
    private Long permId;
    private String permCode;
    private String permName;
    private String description;
    private Date createTime;
}
