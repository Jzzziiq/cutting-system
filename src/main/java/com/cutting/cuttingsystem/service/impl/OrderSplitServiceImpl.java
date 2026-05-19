package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cutting.cuttingsystem.entitys.CabinetOrderItem;
import com.cutting.cuttingsystem.entitys.DTO.SplitConfirmRequest;
import com.cutting.cuttingsystem.entitys.DTO.SplitExecuteRequest;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.entitys.VO.SplitConfirmResultVO;
import com.cutting.cuttingsystem.entitys.VO.SplitItemVO;
import com.cutting.cuttingsystem.mapper.CabinetOrderItemMapper;
import com.cutting.cuttingsystem.service.OrderSplitService;
import com.cutting.cuttingsystem.service.TBoardService;
import com.cutting.cuttingsystem.service.TOrderItemService;
import com.cutting.cuttingsystem.service.TOrderService;
import com.cutting.cuttingsystem.util.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OrderSplitServiceImpl implements OrderSplitService {

    private static final int EDGE_THICKNESS = 1;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TBoardService boardService;
    @Autowired
    private TOrderService orderService;
    @Autowired
    private TOrderItemService orderItemService;
    @Autowired
    private CabinetOrderItemMapper cabinetOrderItemMapper;

    @Override
    public List<SplitItemVO> execute(SplitExecuteRequest request) {
        JsonNode cabinetJson = request.getCabinetJson();
        Map<String, Long> slotMap = request.getMaterialSlotBoardMap();
        JsonNode cabinet = cabinetJson.get("cabinet");
        JsonNode boards = cabinetJson.get("boards");
        if (boards == null || !boards.isArray()) {
            throw new IllegalArgumentException("柜体JSON中缺少boards数组");
        }
        List<SplitItemVO> result = new ArrayList<>();
        for (JsonNode boardNode : boards) {
            result.add(processBoard(boardNode, slotMap, cabinet));
        }
        return result;
    }

    @Override
    @Transactional
    public SplitConfirmResultVO confirm(SplitConfirmRequest request) {
        Long orderId = request.getOrderId();
        TOrder order = orderService.getById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        Long currentUserId = UserContext.getCurrentUserId();
        if (!currentUserId.equals(order.getUserId())) throw new RuntimeException("无权操作此订单");

        JsonNode cabinetJson = request.getCabinetJson();
        Map<String, Long> slotMap = request.getMaterialSlotBoardMap();
        JsonNode cabinet = cabinetJson.get("cabinet");
        JsonNode boards = cabinetJson.get("boards");
        if (boards == null || !boards.isArray()) {
            throw new IllegalArgumentException("柜体JSON中缺少boards数组");
        }

        String cabinetName = cabinet.has("name") ? cabinet.get("name").asText() : "未命名柜体";
        String cabinetCategory = inferCategory(boards);
        String splitBatchCode = generateSplitBatchCode(cabinetCategory);
        List<Long> createdItemIds = new ArrayList<>();

        for (JsonNode boardNode : boards) {
            SplitItemVO vo = processBoard(boardNode, slotMap, cabinet);
            TOrderItem item = toOrderItem(vo, orderId, currentUserId);
            orderItemService.save(item);

            CabinetOrderItem coi = toCabinetOrderItem(vo, item.getItemId(), orderId, currentUserId,
                    splitBatchCode, cabinetName, cabinet, boardNode, cabinetJson);
            cabinetOrderItemMapper.insert(coi);

            createdItemIds.add(item.getItemId());
        }

        SplitConfirmResultVO result = new SplitConfirmResultVO();
        result.setOrderId(orderId);
        result.setSplitBatchCode(splitBatchCode);
        result.setCabinetName(cabinetName);
        result.setCreatedItemIds(createdItemIds);
        result.setNextAction("layout-workbench");
        return result;
    }

    private SplitItemVO processBoard(JsonNode board, Map<String, Long> slotMap, JsonNode cabinet) {
        String boardType = board.has("type") ? board.get("type").asText() : null;
        String displayName = board.has("displayName") ? board.get("displayName").asText() : boardType;
        int designLength = board.get("designLength").asInt();
        int designWidth = board.get("designWidth").asInt();
        int thickness = board.get("thickness").asInt();
        String grain = board.has("grain") ? board.get("grain").asText() : "none";

        // Resolve boardId
        Long boardId = null;
        if (board.has("boardId") && !board.get("boardId").isNull()) {
            boardId = board.get("boardId").asLong();
        } else if (slotMap != null && board.has("materialSlot") && !board.get("materialSlot").isNull()) {
            boardId = slotMap.get(board.get("materialSlot").asText());
        }
        if (boardId == null) {
            throw new RuntimeException("板件" + displayName + "未指定板材");
        }

        // Validate board
        TBoard tBoard = boardService.getById(boardId);
        if (tBoard == null) throw new RuntimeException("板材" + boardId + "不存在");
        if (tBoard.getIsEnabled() != null && tBoard.getIsEnabled() != 1) {
            throw new RuntimeException("板材" + boardId + "已禁用");
        }
        if (!Integer.valueOf(thickness).equals(tBoard.getThickness())) {
            throw new RuntimeException("板件" + displayName + "厚度(" + thickness + ")与板材厚度(" + tBoard.getThickness() + ")不一致");
        }
        boolean fitsRotated = (designLength <= tBoard.getLength() && designWidth <= tBoard.getWidth())
                || (designLength <= tBoard.getWidth() && designWidth <= tBoard.getLength());
        if (!fitsRotated) {
            throw new RuntimeException("板件" + displayName + "尺寸超出板材规格");
        }

        // Edge banding deduction (2D local coords)
        boolean edgeLeft = false, edgeRight = false, edgeTop = false, edgeBottom = false;
        JsonNode eb = board.get("edgeBanding");
        if (eb != null) {
            edgeLeft = eb.has("left") && eb.get("left").asBoolean();
            edgeRight = eb.has("right") && eb.get("right").asBoolean();
            edgeTop = eb.has("top") && eb.get("top").asBoolean();
            edgeBottom = eb.has("bottom") && eb.get("bottom").asBoolean();
        }
        int cutWidth = designWidth - (edgeLeft ? EDGE_THICKNESS : 0) - (edgeRight ? EDGE_THICKNESS : 0);
        int cutLength = designLength - (edgeTop ? EDGE_THICKNESS : 0) - (edgeBottom ? EDGE_THICKNESS : 0);
        if (cutWidth <= 0 || cutLength <= 0) {
            throw new RuntimeException("板件" + displayName + "扣除封边后尺寸非法");
        }

        // Hinge holes
        List<Map<String, Object>> holeOps = new ArrayList<>();
        JsonNode hingeHoles = board.get("hingeHoles");
        if (hingeHoles != null && hingeHoles.isArray()) {
            for (JsonNode hole : hingeHoles) {
                if ("even".equals(hole.has("spacing") ? hole.get("spacing").asText() : null)) {
                    int count = hole.get("count").asInt();
                    int diameter = hole.has("diameter") ? hole.get("diameter").asInt() : 35;
                    int depth = hole.has("depth") ? hole.get("depth").asInt() : 12;
                    int edgeDistance = hole.has("edgeDistance") ? hole.get("edgeDistance").asInt() : 22;
                    int doorGap = hole.has("doorGap") ? hole.get("doorGap").asInt() : 2;
                    String direction = hole.has("direction") ? hole.get("direction").asText() : "height";
                    String opening = hole.has("opening") ? hole.get("opening").asText() : "left";

                    int header = edgeDistance + doorGap;
                    int alongLength = "height".equals(direction) ? designLength : designWidth;
                    int effectiveRange = alongLength - header * 2;

                    for (int i = 0; i < count; i++) {
                        int pos = header + (count == 1 ? effectiveRange / 2 : effectiveRange * i / (count - 1));
                        Map<String, Object> op = new LinkedHashMap<>();
                        op.put("sourceBoardId", board.has("id") ? board.get("id").asText() : "");
                        op.put("type", "hinge_cup");
                        op.put("face", "inner");
                        int holeX = "width".equals(direction) ? pos : edgeDistance;
                        int holeY = "height".equals(direction) ? pos : edgeDistance;
                        op.put("x", holeX);
                        op.put("y", holeY);
                        op.put("diameter", diameter);
                        op.put("depth", depth);
                        op.put("unit", "mm");
                        holeOps.add(op);
                    }
                }
            }
        }

        // Edge role from JSON
        Map<String, String> edgeRole = new LinkedHashMap<>();
        JsonNode er = board.get("edgeRole");
        if (er != null) {
            er.fieldNames().forEachRemaining(k -> edgeRole.put(k, er.get(k).asText()));
        }

        SplitItemVO vo = new SplitItemVO();
        vo.setPartName(displayName);
        vo.setBoardType(boardType);
        vo.setBoardId(boardId);
        vo.setMaterialName(tBoard.getMaterialType());
        vo.setColor(tBoard.getColor());
        vo.setLength(cutLength);
        vo.setWidth(cutWidth);
        vo.setThickness(thickness);
        vo.setQuantity(1);
        vo.setEdgeLeft(edgeLeft ? 1 : 0);
        vo.setEdgeRight(edgeRight ? 1 : 0);
        vo.setEdgeTop(edgeTop ? 1 : 0);
        vo.setEdgeBottom(edgeBottom ? 1 : 0);
        vo.setEdgeRole(edgeRole);
        vo.setGrain(grain);
        vo.setHoleOperations(holeOps.isEmpty() ? null : holeOps);
        return vo;
    }

    private TOrderItem toOrderItem(SplitItemVO vo, Long orderId, Long currentUserId) {
        TOrderItem item = new TOrderItem();
        item.setOrderId(orderId);
        item.setPartName(vo.getPartName());
        item.setBoardId(vo.getBoardId());
        item.setLength(vo.getLength());
        item.setWidth(vo.getWidth());
        item.setThickness(vo.getThickness());
        item.setQuantity(vo.getQuantity());
        item.setMaterialName(vo.getMaterialName());
        item.setColor(vo.getColor());
        item.setEdgeLeft(vo.getEdgeLeft());
        item.setEdgeRight(vo.getEdgeRight());
        item.setEdgeTop(vo.getEdgeTop());
        item.setEdgeBottom(vo.getEdgeBottom());
        item.setEdgeFront(0);
        item.setEdgeBack(0);
        item.setIsTexture("vertical".equals(vo.getGrain()) || "horizontal".equals(vo.getGrain()) ? 1 : 0);
        item.setAllowRotation(0);
        return item;
    }

    private CabinetOrderItem toCabinetOrderItem(SplitItemVO vo, Long itemId, Long orderId,
            Long currentUserId, String splitBatchCode, String cabinetName,
            JsonNode cabinet, JsonNode boardNode, JsonNode cabinetJson) {
        CabinetOrderItem coi = new CabinetOrderItem();
        coi.setOrderItemId(itemId);
        coi.setOrderId(orderId);
        coi.setUserId(currentUserId);
        coi.setSplitBatchCode(splitBatchCode);
        coi.setSourceBoardId(boardNode.has("id") ? boardNode.get("id").asText() : null);
        coi.setWorkpieceCode(vo.getPartCode());
        coi.setCabinetName(cabinetName);
        coi.setRoom(cabinet.has("room") ? cabinet.get("room").asText() : null);
        coi.setPurpose(cabinet.has("purpose") ? cabinet.get("purpose").asText() : null);
        coi.setBoardType(vo.getBoardType());
        coi.setThickness(vo.getThickness());
        coi.setGrainDirection(vo.getGrain());
        coi.setDesignLength(vo.getLength());
        coi.setDesignWidth(vo.getWidth());
        if (boardNode.has("position")) {
            JsonNode pos = boardNode.get("position");
            coi.setPositionX(pos.has("x") ? pos.get("x").asDouble() : null);
            coi.setPositionY(pos.has("y") ? pos.get("y").asDouble() : null);
            coi.setPositionZ(pos.has("z") ? pos.get("z").asDouble() : null);
        }
        try {
            JsonNode eb = boardNode.get("edgeBanding");
            coi.setEdgeBanding(eb != null ? objectMapper.writeValueAsString(eb) : null);
            JsonNode er = boardNode.get("edgeRole");
            coi.setEdgeRole(er != null ? objectMapper.writeValueAsString(er) : null);
            coi.setHoleOperations(vo.getHoleOperations() != null ? objectMapper.writeValueAsString(vo.getHoleOperations()) : null);
            coi.setSourceBoardJson(objectMapper.writeValueAsString(boardNode));
        } catch (Exception ignored) {
        }
        return coi;
    }

    private String generateSplitBatchCode(String category) {
        String prefix = "wardrobe".equals(category) ? "ZG" : "DG";
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        QueryWrapper<CabinetOrderItem> qw = new QueryWrapper<>();
        qw.likeRight("split_batch_code", prefix + "-" + date);
        Long count = cabinetOrderItemMapper.selectCount(qw);
        return prefix + "-" + date + "-" + String.format("%03d", (count != null ? count : 0) + 1);
    }

    private String inferCategory(JsonNode boards) {
        if (boards != null && boards.isArray()) {
            for (JsonNode b : boards) {
                if ("door".equals(b.has("type") ? b.get("type").asText() : "")) return "wardrobe";
            }
        }
        return "base-cabinet";
    }
}
