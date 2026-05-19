package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.List;

@Data
public class LayoutInputVO {
    private List<GroupVO> groups;
    private AlgorithmConfigVO algorithmConfig;

    @Data
    public static class GroupVO {
        private BoardVO board;
        private List<ItemVO> items;
    }

    @Data
    public static class BoardVO {
        private Long boardId;
        private Integer length;
        private Integer width;
        private Integer thickness;
        private String brand;
        private String materialType;
        private String color;
        private String sizeType;
    }

    @Data
    public static class ItemVO {
        private Long orderItemId;
        private String partCode;
        private String partName;
        private Integer length;
        private Integer width;
        private Integer quantity;
    }

    @Data
    public static class AlgorithmConfigVO {
        private int gapDistance = 3;
        private boolean allowRotation = false;
    }
}
