package com.cutting.cuttingsystem.entitys.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Map;

@Data
public class SplitConfirmRequest {
    @NotNull(message = "orderId is required")
    @Positive(message = "orderId must be greater than 0")
    private Long orderId;
    @NotBlank(message = "confirmMode is required")
    private String confirmMode;
    @NotNull(message = "cabinetJson is required")
    private JsonNode cabinetJson;
    private Map<String, Long> materialSlotBoardMap;
}
