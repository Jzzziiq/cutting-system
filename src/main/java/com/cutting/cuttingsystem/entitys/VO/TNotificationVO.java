package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;
import java.util.Date;

@Data
public class TNotificationVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long taskId;
    private Integer isRead;
    private Date createdAt;
}
