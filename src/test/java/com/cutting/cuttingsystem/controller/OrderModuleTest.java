package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.entitys.VO.TOrderItemVO;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;
import com.cutting.cuttingsystem.service.TOrderService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TOrderService orderService;

    @Test
    void pageReturnsOrderRecords() throws Exception {
        Page<TOrder> page = new Page<>(1, 10);
        page.setRecords(List.of(order(1L)));
        page.setTotal(1);
        when(orderService.page(any())).thenReturn(page);

        mockMvc.perform(get("/orders")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].orderId").value(1))
                .andExpect(jsonPath("$.data.records[0].processName").value("cabinet-door-cutting"));
    }

    @Test
    void pageRejectsMissingPageSize() throws Exception {
        mockMvc.perform(get("/orders")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }

    @Test
    void pageRejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/orders")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }

    @Test
    void getByIdReturnsOrderDetailWithItems() throws Exception {
        when(orderService.getOrderDetail(1L)).thenReturn(orderVO(1L));

        mockMvc.perform(get("/orders/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.items[0].partName").value("left-door"));
    }

    @Test
    void getByIdReturnsBusinessErrorWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderDetail(999L)).thenReturn(null);

        mockMvc.perform(get("/orders/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("order not found"));
    }

    @Test
    void getByIdRejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/orders/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createOrderReturnsCreatedOrderWhenRequestIsValid() throws Exception {
        when(orderService.createOrder(any())).thenReturn(orderVO(1L));

        mockMvc.perform(post("/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void createOrderRejectsMissingCustomerId() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "processName": "cabinet-door-cutting"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.customerId").exists());
    }

    @Test
    void createOrderRejectsBlankProcessName() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "processName": " "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.processName").exists());
    }

    @Test
    void createOrderRejectsNestedItemWithZeroWidth() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "processName": "cabinet-door-cutting",
                                  "items": [
                                    {
                                      "partName": "left-door",
                                      "width": 0,
                                      "length": 500,
                                      "thickness": 18,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data['items[0].width']").exists());
    }

    @Test
    void createOrderRejectsOverlongRemark() throws Exception {
        String overlongRemark = "A".repeat(256);

        mockMvc.perform(post("/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "processName": "cabinet-door-cutting",
                                  "remark": "%s"
                                }
                                """.formatted(overlongRemark)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.remark").exists());
    }

    @Test
    void updateOrderReturnsSuccessWhenRequestIsValid() throws Exception {
        when(orderService.updateOrder(eq(1L), any())).thenReturn(true);

        mockMvc.perform(put("/orders/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateOrderReturnsBusinessErrorWhenUpdateFails() throws Exception {
        when(orderService.updateOrder(eq(999L), any())).thenReturn(false);

        mockMvc.perform(put("/orders/999")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("update order failed"));
    }

    @Test
    void updateOrderRejectsInvalidStatus() throws Exception {
        mockMvc.perform(put("/orders/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "processName": "cabinet-door-cutting",
                                  "orderStatus": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderStatus").exists());
    }

    @Test
    void updateOrderRejectsNestedItemWithInvalidEdgeFlag() throws Exception {
        mockMvc.perform(put("/orders/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "processName": "cabinet-door-cutting",
                                  "items": [
                                    {
                                      "partName": "left-door",
                                      "width": 300,
                                      "length": 500,
                                      "thickness": 18,
                                      "quantity": 2,
                                      "edgeLeft": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data['items[0].edgeLeft']").exists());
    }

    @Test
    void deleteOrderReturnsSuccessWhenRemoveSucceeds() throws Exception {
        when(orderService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/orders/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteOrderReturnsBusinessErrorWhenRemoveFails() throws Exception {
        when(orderService.removeById(999L)).thenReturn(false);

        mockMvc.perform(delete("/orders/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("delete order failed"));
    }

    @Test
    void protectedOrderEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/orders")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    private TOrder order(Long id) {
        TOrder order = new TOrder();
        order.setOrderId(id);
        order.setOrderNo("ORD202605010001");
        order.setCustomerId(1L);
        order.setCustomerName("ACME Furniture");
        order.setProcessName("cabinet-door-cutting");
        order.setOrderStatus(0);
        return order;
    }

    private TOrderVO orderVO(Long id) {
        TOrderVO orderVO = new TOrderVO();
        orderVO.setOrderId(id);
        orderVO.setOrderNo("ORD202605010001");
        orderVO.setCustomerId(1L);
        orderVO.setCustomerName("ACME Furniture");
        orderVO.setProcessName("cabinet-door-cutting");
        orderVO.setOrderStatus(0);
        orderVO.setItems(List.of(orderItemVO()));
        return orderVO;
    }

    private TOrderItemVO orderItemVO() {
        TOrderItemVO itemVO = new TOrderItemVO();
        itemVO.setItemId(1L);
        itemVO.setOrderId(1L);
        itemVO.setPartName("left-door");
        itemVO.setWidth(300);
        itemVO.setLength(500);
        itemVO.setThickness(18);
        itemVO.setQuantity(2);
        return itemVO;
    }

    private String validOrderJson() {
        return """
                {
                  "customerId": 1,
                  "processName": "cabinet-door-cutting",
                  "orderStatus": 0,
                  "items": [
                    {
                      "partName": "left-door",
                      "width": 300,
                      "length": 500,
                      "thickness": 18,
                      "quantity": 2,
                      "edgeLeft": 1
                    }
                  ],
                  "remark": "normal order"
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
