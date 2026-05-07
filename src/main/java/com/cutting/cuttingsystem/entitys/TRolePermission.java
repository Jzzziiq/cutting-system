package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_role_permission")
public class TRolePermission implements Serializable {
    @TableId
    private Long id;
    private Long roleId;
    private Long permId;
    private Date createTime;
}
