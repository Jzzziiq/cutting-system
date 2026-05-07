package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignRoleDTO {
    @NotNull
    private Long userId;
    private List<Long> roleIds;
}
