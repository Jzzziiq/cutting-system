package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SplitItemVO {
    private String partCode;
    private String partName;
    private Long boardId;
    private String materialName;
    private String color;
    private Integer length;
    private Integer width;
    private Integer thickness;
    private Integer quantity;
    private Integer edgeLeft;
    private Integer edgeRight;
    private Integer edgeTop;
    private Integer edgeBottom;
    private Map<String, String> edgeRole;
    private String grain;
    private String boardType;
    private List<Map<String, Object>> holeOperations;
}
