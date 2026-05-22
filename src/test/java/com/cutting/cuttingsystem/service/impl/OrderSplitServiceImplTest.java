package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cutting.cuttingsystem.entitys.CabinetOrderItem;
import com.cutting.cuttingsystem.entitys.DTO.SplitConfirmRequest;
import com.cutting.cuttingsystem.entitys.DTO.SplitExecuteRequest;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.entitys.VO.SplitConfirmResultVO;
import com.cutting.cuttingsystem.entitys.VO.SplitItemVO;
import com.cutting.cuttingsystem.mapper.CabinetOrderItemMapper;
import com.cutting.cuttingsystem.service.TBoardService;
import com.cutting.cuttingsystem.service.TOrderItemService;
import com.cutting.cuttingsystem.service.TOrderService;
import com.cutting.cuttingsystem.util.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSplitServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @InjectMocks
    private OrderSplitServiceImpl orderSplitService;

    @Mock
    private TBoardService boardService;
    @Mock
    private TOrderService orderService;
    @Mock
    private TOrderItemService orderItemService;
    @Mock
    private CabinetOrderItemMapper cabinetOrderItemMapper;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUserId(2L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void executeGeneratesNextWorkpieceCodeWithinOrder() throws Exception {
        mockBoard();
        mockExistingOrderCodes("ZG-002");

        SplitExecuteRequest request = new SplitExecuteRequest();
        request.setCabinetJson(cabinetJson());
        request.setMaterialSlotBoardMap(Map.of("cabinet_body", 1L));

        List<SplitItemVO> items = orderSplitService.execute(request);

        assertEquals("ZG-003", items.get(0).getPartCode());
    }

    @Test
    void executeMatchesCabinetPanelByThicknessRangeOnly() throws Exception {
        mockBoard(20, "任意板材");

        SplitExecuteRequest request = new SplitExecuteRequest();
        request.setCabinetJson(cabinetJson());
        request.setMaterialSlotBoardMap(Map.of("cabinet_body", 1L));

        List<SplitItemVO> items = orderSplitService.execute(request);

        assertEquals("任意板材", items.get(0).getMaterialName());
        assertEquals(20, items.get(0).getThickness());
    }

    @Test
    void confirmPersistsGeneratedWorkpieceCodeToOrderItemAndCabinetItem() throws Exception {
        mockBoard();
        mockExistingOrderCodes("ZG-002");
        TOrder order = new TOrder();
        order.setOrderId(2L);
        order.setUserId(2L);
        when(orderService.getById(2L)).thenReturn(order);
        when(orderItemService.save(any(TOrderItem.class))).thenAnswer(invocation -> {
            TOrderItem item = invocation.getArgument(0);
            item.setItemId(88L);
            return true;
        });
        when(cabinetOrderItemMapper.insert(any(CabinetOrderItem.class))).thenReturn(1);

        SplitConfirmRequest request = new SplitConfirmRequest();
        request.setOrderId(2L);
        request.setConfirmMode("append");
        request.setCabinetJson(cabinetJson());
        request.setMaterialSlotBoardMap(Map.of("cabinet_body", 1L));

        SplitConfirmResultVO result = orderSplitService.confirm(request);

        ArgumentCaptor<TOrderItem> orderItemCaptor = ArgumentCaptor.forClass(TOrderItem.class);
        ArgumentCaptor<CabinetOrderItem> cabinetItemCaptor = ArgumentCaptor.forClass(CabinetOrderItem.class);
        org.mockito.Mockito.verify(orderItemService).save(orderItemCaptor.capture());
        org.mockito.Mockito.verify(cabinetOrderItemMapper).insert(cabinetItemCaptor.capture());

        assertEquals("ZG-003", orderItemCaptor.getValue().getPartCode());
        assertEquals("ZG-003", cabinetItemCaptor.getValue().getWorkpieceCode());
        assertTrue(result.getCreatedItemIds().contains(88L));
    }

    private void mockBoard() {
        mockBoard(18, "颗粒板");
    }

    private void mockBoard(int thickness, String materialType) {
        TBoard board = new TBoard();
        board.setBoardId(1L);
        board.setMaterialType(materialType);
        board.setColor("暖白色");
        board.setLength(2440);
        board.setWidth(1220);
        board.setThickness(thickness);
        board.setIsEnabled(1);
        when(boardService.getById(1L)).thenReturn(board);
    }

    private void mockExistingOrderCodes(String partCode) {
        TOrderItem existing = new TOrderItem();
        existing.setPartCode(partCode);
        when(orderItemService.getOne(org.mockito.ArgumentMatchers.<Wrapper<TOrderItem>>any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(existing);
    }

    private JsonNode cabinetJson() throws Exception {
        return OBJECT_MAPPER.readTree("""
            {
              "cabinet": { "name": "衣柜", "category": "wardrobe", "orderId": 2 },
              "boards": [
                {
                  "id": "b-001",
                  "type": "side",
                  "displayName": "左侧板",
                  "materialSlot": "cabinet_body",
                  "designLength": 2200,
                  "designWidth": 600,
                  "thickness": 18,
                  "grain": "vertical",
                  "edgeBanding": { "left": false, "right": false, "top": true, "bottom": true },
                  "hingeHoles": []
                }
              ]
            }
            """);
    }
}
