package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.io.Serializable;

@Data
public class TCustomerVO implements Serializable {
    private Long customerId;
    private String customerName;
    private String phone;
    private String address;
    private Integer isEnabled;
    private String remark;
}
