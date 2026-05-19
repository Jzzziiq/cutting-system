package com.cutting.cuttingsystem.entitys.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SplitExecuteRequest {
    @NotNull(message = "cabinetJson is required")
    private JsonNode cabinetJson;
    private Map<String, Long> materialSlotBoardMap;
}
