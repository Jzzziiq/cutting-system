package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class TCustomerDTO implements Serializable {
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    private String address;

    private String remark;
}
