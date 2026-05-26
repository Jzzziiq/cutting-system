package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_notification")
public class TNotification implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long taskId;
    private Integer isRead;
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
