package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_role")
public class TRole implements Serializable {
    @TableId
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private Date createTime;
}
