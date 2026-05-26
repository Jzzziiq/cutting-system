package com.cutting.cuttingsystem.entitys.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TOrderVO {
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private String customerAddress;
    private String processName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date dispatchDate;
    private Integer orderStatus;
    private String statusLabel;
    private String statusHistory;
    private String rawMaterialJson;
    private String remnantJson;
    private String configJson;
    private Long layoutResultId;
    private Date createTime;
    private Date updateTime;
    private Date finishTime;
    private String remark;
    private List<TOrderItemVO> items;
}
