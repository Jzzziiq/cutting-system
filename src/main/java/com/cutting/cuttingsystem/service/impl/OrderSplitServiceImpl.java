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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OrderSplitServiceImpl implements OrderSplitService {

    private static final Logger log = LoggerFactory.getLogger(OrderSplitServiceImpl.class);
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

        Map<Long, TBoard> boardCache = resolveBoardCache(boards, slotMap);
        String prefix = workpiecePrefix(inferCategory(cabinet, boards));
        int nextSequence = nextWorkpieceSequence(extractOrderId(cabinet), prefix);
        List<SplitItemVO> result = new ArrayList<>();
        for (JsonNode boardNode : boards) {
            result.add(processBoard(boardNode, slotMap, cabinet, boardCache, formatWorkpieceCode(prefix, nextSequence++)));
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
        String cabinetCategory = inferCategory(cabinet, boards);
        String workpiecePrefix = workpiecePrefix(cabinetCategory);
        String splitBatchCode = generateSplitBatchCode(cabinetCategory);
        int nextSequence = nextWorkpieceSequence(orderId, workpiecePrefix);
        // Pre-load all referenced boards to avoid N+1
        Map<Long, TBoard> boardCache = resolveBoardCache(boards, slotMap);

        List<Long> createdItemIds = new ArrayList<>();

        for (JsonNode boardNode : boards) {
            SplitItemVO vo = processBoard(boardNode, slotMap, cabinet, boardCache,
                    formatWorkpieceCode(workpiecePrefix, nextSequence++));
            TOrderItem item = toOrderItem(vo, orderId);
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

    private SplitItemVO processBoard(JsonNode board, Map<String, Long> slotMap, JsonNode cabinet,
                                      Map<Long, TBoard> boardCache, String partCode) {
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

        // Validate board (from cache)
        TBoard tBoard = boardCache.get(boardId);
        if (tBoard == null) throw new RuntimeException("板材" + boardId + "不存在");
        if (tBoard.getIsEnabled() != null && tBoard.getIsEnabled() != 1) {
            throw new RuntimeException("板材" + boardId + "已禁用");
        }
        if (thickness != tBoard.getThickness().intValue()) {
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
                if (hole.has("spacing") && "even".equals(hole.get("spacing").asText())) {
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
                        op.put("workpieceCode", partCode);
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
        vo.setPartCode(partCode);
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

    private TOrderItem toOrderItem(SplitItemVO vo, Long orderId) {
        TOrderItem item = new TOrderItem();
        item.setOrderId(orderId);
        item.setPartName(vo.getPartName());
        item.setPartCode(vo.getPartCode());
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
        } catch (Exception e) { log.warn("封边JSON序列化失败 board={}", boardNode.has("id") ? boardNode.get("id").asText() : "?", e); }
        try {
            JsonNode er = boardNode.get("edgeRole");
            coi.setEdgeRole(er != null ? objectMapper.writeValueAsString(er) : null);
        } catch (Exception e) { log.warn("edge_role序列化失败", e); }
        try {
            coi.setHoleOperations(vo.getHoleOperations() != null ? objectMapper.writeValueAsString(vo.getHoleOperations()) : null);
        } catch (Exception e) { log.warn("hole_operations序列化失败", e); }
        try {
            coi.setSourceBoardJson(objectMapper.writeValueAsString(boardNode));
        } catch (Exception e) { log.warn("source_board_json序列化失败", e); }
        return coi;
    }

    private String generateSplitBatchCode(String category) {
        String prefix = "wardrobe".equals(category) ? "ZG" : "DG";
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        QueryWrapper<CabinetOrderItem> qw = new QueryWrapper<>();
        qw.select("split_batch_code")
                .likeRight("split_batch_code", prefix + "-" + date)
                .groupBy("split_batch_code");
        List<CabinetOrderItem> distinctBatches = cabinetOrderItemMapper.selectList(qw);
        int batchCount = distinctBatches != null ? distinctBatches.size() : 0;
        return prefix + "-" + date + "-" + String.format("%03d", batchCount + 1);
    }

    private String inferCategory(JsonNode cabinet, JsonNode boards) {
        if (cabinet != null && cabinet.has("category")) {
            String category = cabinet.get("category").asText();
            if ("base-cabinet".equals(category) || "wardrobe".equals(category)) {
                return category;
            }
        }
        if (cabinet != null && cabinet.has("name")) {
            String name = cabinet.get("name").asText();
            if (name.contains("地柜")) return "base-cabinet";
            if (name.contains("衣柜")) return "wardrobe";
        }
        if (boards != null && boards.isArray()) {
            for (JsonNode b : boards) {
                if ("door".equals(b.has("type") ? b.get("type").asText() : "")) return "wardrobe";
            }
        }
        return "base-cabinet";
    }

    private Long extractOrderId(JsonNode cabinet) {
        if (cabinet == null || !cabinet.has("orderId") || cabinet.get("orderId").isNull()) {
            return null;
        }
        long orderId = cabinet.get("orderId").asLong();
        return orderId > 0 ? orderId : null;
    }

    private String workpiecePrefix(String category) {
        return "wardrobe".equals(category) ? "ZG" : "DG";
    }

    private String formatWorkpieceCode(String prefix, int sequence) {
        return prefix + "-" + String.format("%03d", sequence);
    }

    private int nextWorkpieceSequence(Long orderId, String prefix) {
        if (orderId == null) {
            return 1;
        }
        QueryWrapper<TOrderItem> qw = new QueryWrapper<>();
        qw.select("MAX(part_code) as part_code")
                .eq("order_id", orderId)
                .likeRight("part_code", prefix + "-");
        TOrderItem maxItem = orderItemService.getOne(qw, false);
        if (maxItem == null || maxItem.getPartCode() == null) {
            return 1;
        }
        return parseWorkpieceSequence(maxItem.getPartCode(), prefix) + 1;
    }

    private Map<Long, TBoard> resolveBoardCache(JsonNode boards, Map<String, Long> slotMap) {
        Map<Long, TBoard> cache = new HashMap<>();
        if (boards == null || !boards.isArray()) return cache;
        for (JsonNode board : boards) {
            Long boardId = null;
            if (board.has("boardId") && !board.get("boardId").isNull()) {
                boardId = board.get("boardId").asLong();
            } else if (slotMap != null && board.has("materialSlot") && !board.get("materialSlot").isNull()) {
                boardId = slotMap.get(board.get("materialSlot").asText());
            }
            if (boardId != null && !cache.containsKey(boardId)) {
                TBoard tBoard = boardService.getById(boardId);
                if (tBoard != null) cache.put(boardId, tBoard);
            }
        }
        return cache;
    }

    private int parseWorkpieceSequence(String partCode, String prefix) {
        String codePrefix = prefix + "-";
        if (partCode == null || !partCode.startsWith(codePrefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(partCode.substring(codePrefix.length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
