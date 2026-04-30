package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TLayoutResultVO {
    private Long resultId;
    private Long orderId;
    private BigDecimal usageRate;
    private BigDecimal totalArea;
    private Integer containerCount;
    private String resultJson;
    private String imagePath;
    private String ncFilePath;
    private String labelFilePath;
    private Date createTime;
}
