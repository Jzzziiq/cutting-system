package com.cutting.cuttingsystem.entitys.VO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class TCustomerVO implements Serializable {
    private Long customerId;

    @NotBlank(message = "customerName is required")
    @Size(max = 100, message = "customerName must be at most 100 characters")
    private String customerName;

    @NotBlank(message = "phone is required")
    @Size(max = 20, message = "phone must be at most 20 characters")
    private String phone;

    @Size(max = 255, message = "address must be at most 255 characters")
    private String address;

    @Min(value = 0, message = "isEnabled must be 0 or 1")
    @Max(value = 1, message = "isEnabled must be 0 or 1")
    private Integer isEnabled;

    @Size(max = 255, message = "remark must be at most 255 characters")
    private String remark;
}
