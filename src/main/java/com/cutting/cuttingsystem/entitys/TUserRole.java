package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_user_role")
public class TUserRole implements Serializable {
    @TableId
    private Long id;
    private Long userId;
    private Long roleId;
    private Date createTime;
}
