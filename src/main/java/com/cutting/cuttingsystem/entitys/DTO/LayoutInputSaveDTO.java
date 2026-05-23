package com.cutting.cuttingsystem.entitys.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class LayoutInputSaveDTO {
    private List<GroupDTO> groups;

    @Data
    public static class GroupDTO {
        @NotNull(message = "boardId must not be null")
        @Positive(message = "boardId must be greater than 0")
        private Long boardId;

        @NotNull(message = "items must not be null")
        @Valid
        private List<ItemDTO> items;
    }

    @Data
    public static class ItemDTO {
        private String partName;
        private String partCode;
        @NotNull(message = "length must not be null")
        @Positive(message = "length must be greater than 0")
        private Integer length;
        @NotNull(message = "width must not be null")
        @Positive(message = "width must be greater than 0")
        private Integer width;
        @NotNull(message = "quantity must not be null")
        @Positive(message = "quantity must be greater than 0")
        private Integer quantity;
        private Integer isTexture;
        private Integer allowRotation;
        private Integer edgeLeft;
        private Integer edgeRight;
        private Integer edgeTop;
        private Integer edgeBottom;
        private String remark;
    }
}
