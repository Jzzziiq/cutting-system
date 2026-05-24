package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_algorithm_task")
public class AlgorithmTask implements Serializable {
    @TableId
    private String taskId;
    private Long userId;
    private Long orgId;
    private String algorithm;
    private Integer status;         // 0 pending, 1 running, 2 completed, -1 failed
    private String inputJson;
    private String resultJson;
    private Double bestRate;
    private Integer containerCount;
    private Long durationMs;
    private String errorMsg;
    private Date createTime;
    private Date completeTime;
}
