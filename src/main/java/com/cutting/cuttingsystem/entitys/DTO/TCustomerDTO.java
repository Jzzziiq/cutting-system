package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class TCustomerDTO implements Serializable {
    @NotBlank(message = "customerName is required")
    @Size(max = 100, message = "customerName must be at most 100 characters")
    private String customerName;

    @NotBlank(message = "phone is required")
    @Size(max = 20, message = "phone must be at most 20 characters")
    private String phone;

    @Size(max = 255, message = "address must be at most 255 characters")
    private String address;

    @Size(max = 255, message = "remark must be at most 255 characters")
    private String remark;
}
