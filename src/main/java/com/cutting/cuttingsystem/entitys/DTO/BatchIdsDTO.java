package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchIdsDTO implements Serializable {
    @NotEmpty(message = "ids must not be empty")
    @Size(max = 100, message = "ids must be at most 100")
    private List<@NotNull(message = "id is required") @Positive(message = "id must be greater than 0") Long> ids;
}
