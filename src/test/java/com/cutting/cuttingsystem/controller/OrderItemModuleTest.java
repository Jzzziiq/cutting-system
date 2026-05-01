package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TOrderItemService;
import com.cutting.cuttingsystem.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderItemModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TOrderItemService orderItemService;

    @Test
    void pageReturnsOrderItemRecords() throws Exception {
        Page<TOrderItem> page = new Page<>(1, 10);
        page.setRecords(List.of(orderItem(1L)));
        page.setTotal(1);
        when(orderItemService.page(any())).thenReturn(page);

        mockMvc.perform(get("/order-items")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].itemId").value(1))
                .andExpect(jsonPath("$.data.records[0].partName").value("left-door"));
    }

    @Test
    void pageAllowsMaximumPageSize() throws Exception {
        when(orderItemService.page(any())).thenReturn(new Page<TOrderItem>(1, 100));

        mockMvc.perform(get("/order-items")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void pageRejectsPageNumBelowMinimum() throws Exception {
        mockMvc.perform(get("/order-items")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").exists());
    }

    @Test
    void getByIdReturnsOrderItemDetail() throws Exception {
        when(orderItemService.getById(1L)).thenReturn(orderItem(1L));

        mockMvc.perform(get("/order-items/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.itemId").value(1))
                .andExpect(jsonPath("$.data.width").value(300));
    }

    @Test
    void getByIdReturnsBusinessErrorWhenOrderItemDoesNotExist() throws Exception {
        when(orderItemService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/order-items/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("order item not found"));
    }

    @Test
    void getByIdRejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/order-items/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createOrderItemReturnsCreatedItemWhenRequestIsValid() throws Exception {
        when(orderItemService.save(any(TOrderItem.class))).thenReturn(true);

        mockMvc.perform(post("/order-items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderItemJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.partName").value("left-door"));
    }

    @Test
    void createOrderItemRejectsMissingOrderId() throws Exception {
        mockMvc.perform(post("/order-items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "partName": "left-door",
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("orderId is required"));
    }

    @Test
    void createOrderItemRejectsBlankPartName() throws Exception {
        mockMvc.perform(post("/order-items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "partName": " ",
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.partName").exists());
    }

    @Test
    void createOrderItemRejectsZeroLength() throws Exception {
        mockMvc.perform(post("/order-items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "partName": "left-door",
                                  "width": 300,
                                  "length": 0,
                                  "thickness": 18,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length").exists());
    }

    @Test
    void createOrderItemRejectsInvalidEdgeFlag() throws Exception {
        mockMvc.perform(post("/order-items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "partName": "left-door",
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "quantity": 2,
                                  "edgeRight": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.edgeRight").exists());
    }

    @Test
    void createOrderItemRejectsOverlongPartName() throws Exception {
        String overlongPartName = "A".repeat(101);

        mockMvc.perform(post("/order-items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "partName": "%s",
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "quantity": 2
                                }
                                """.formatted(overlongPartName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.partName").exists());
    }

    @Test
    void updateOrderItemReturnsSuccessWhenRequestIsValid() throws Exception {
        when(orderItemService.updateById(any(TOrderItem.class))).thenReturn(true);

        mockMvc.perform(put("/order-items/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderItemJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateOrderItemReturnsBusinessErrorWhenUpdateFails() throws Exception {
        when(orderItemService.updateById(any(TOrderItem.class))).thenReturn(false);

        mockMvc.perform(put("/order-items/999")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderItemJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("update order item failed"));
    }

    @Test
    void updateOrderItemRejectsMissingQuantity() throws Exception {
        mockMvc.perform(put("/order-items/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "partName": "left-door",
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.quantity").exists());
    }

    @Test
    void updateOrderItemRejectsInvalidOffcutId() throws Exception {
        mockMvc.perform(put("/order-items/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "partName": "left-door",
                                  "offcutId": 0,
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.offcutId").exists());
    }

    @Test
    void deleteOrderItemReturnsSuccessWhenRemoveSucceeds() throws Exception {
        when(orderItemService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/order-items/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteOrderItemReturnsBusinessErrorWhenRemoveFails() throws Exception {
        when(orderItemService.removeById(999L)).thenReturn(false);

        mockMvc.perform(delete("/order-items/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("delete order item failed"));
    }

    @Test
    void protectedOrderItemEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/order-items")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    private TOrderItem orderItem(Long id) {
        TOrderItem orderItem = new TOrderItem();
        orderItem.setItemId(id);
        orderItem.setOrderId(1L);
        orderItem.setPartName("left-door");
        orderItem.setWidth(300);
        orderItem.setLength(500);
        orderItem.setThickness(18);
        orderItem.setQuantity(2);
        orderItem.setEdgeLeft(1);
        return orderItem;
    }

    private String validOrderItemJson() {
        return """
                {
                  "orderId": 1,
                  "partName": "left-door",
                  "partCode": "LD-001",
                  "width": 300,
                  "length": 500,
                  "thickness": 18,
                  "quantity": 2,
                  "edgeLeft": 1,
                  "allowRotation": 1,
                  "remark": "normal part"
                }
                """;
    }

    private String bearerToken() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("admin");
        return "Bearer " + jwtUtil.generateToken(user);
    }
}
