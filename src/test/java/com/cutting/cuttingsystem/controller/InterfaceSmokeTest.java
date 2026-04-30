package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import com.cutting.cuttingsystem.entitys.TOffcut;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;
import com.cutting.cuttingsystem.service.TLayoutResultService;
import com.cutting.cuttingsystem.service.TOffcutService;
import com.cutting.cuttingsystem.service.TOrderService;
import com.cutting.cuttingsystem.service.TBoardService;
import com.cutting.cuttingsystem.service.TCustomerService;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterfaceSmokeTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TUserService tUserService;

    @MockitoBean
    private TBoardService tBoardService;

    @MockitoBean
    private TCustomerService customerService;

    @MockitoBean
    private TOffcutService offcutService;

    @MockitoBean
    private TOrderService orderService;

    @MockitoBean
    private TLayoutResultService layoutResultService;

    @Test
    void loginReturnsSuccessResponse() throws Exception {
        when(tUserService.login("admin", "123456"))
                .thenReturn(new LoginInfo(1L, "admin", "管理员", "mock-token"));

        mockMvc.perform(post("/auth/login")
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.token").value("mock-token"));
    }

    @Test
    void algorithmAnswerReturnsSolutionList() throws Exception {
        String requestBody = """
                {
                  "L": 10,
                  "W": 10,
                  "rotateEnable": false,
                  "gapDistance": 0,
                  "squareList": [
                    { "id": "square-1", "l": 5, "w": 5 }
                  ]
                }
                """;

        mockMvc.perform(post("/algorithm/answer")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].containerLength").value(10.0))
                .andExpect(jsonPath("$[0].placeSquareList[0].l").value(5.0));
    }

    @Test
    void boardPageEndpointReturnsPagedResult() throws Exception {
        Page<TBoard> page = new Page<>(1, 10);
        TBoard board = new TBoard();
        board.setBoardId(1L);
        board.setBrand("测试品牌");
        board.setWidth(1220);
        board.setLength(2440);
        page.setRecords(List.of(board));
        page.setTotal(1);
        when(tBoardService.page(any())).thenReturn(page);

        mockMvc.perform(get("/boards")
                        .header("Authorization", "Bearer " + token())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].boardId").value(1));
    }

    @Test
    void boardUpdateEndpointUsesPutWithResourceId() throws Exception {
        when(tBoardService.updateById(any(TBoard.class))).thenReturn(true);

        mockMvc.perform(put("/boards/1")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "测试品牌",
                                  "materialType": "颗粒板",
                                  "color": "白色",
                                  "sizeType": "标准板",
                                  "width": 1220,
                                  "length": 2440,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void customerPageEndpointReturnsPagedResult() throws Exception {
        Page<TCustomer> page = new Page<>(1, 10);
        TCustomer customer = new TCustomer();
        customer.setCustomerId(1L);
        customer.setCustomerName("测试客户");
        customer.setPhone("13800000000");
        page.setRecords(List.of(customer));
        page.setTotal(1);
        when(customerService.page(any())).thenReturn(page);

        mockMvc.perform(get("/customers")
                        .header("Authorization", "Bearer " + token())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].customerId").value(1));
    }

    @Test
    void customerUpdateEndpointUsesPutWithResourceId() throws Exception {
        when(customerService.updateById(any(TCustomer.class))).thenReturn(true);

        mockMvc.perform(put("/customers/1")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "测试客户",
                                  "phone": "13800000000",
                                  "address": "测试地址"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void remnantPageEndpointReturnsPagedResult() throws Exception {
        Page<TOffcut> page = new Page<>(1, 10);
        TOffcut offcut = new TOffcut();
        offcut.setOffcutId(1L);
        offcut.setBoardId(1L);
        offcut.setWidth(300);
        offcut.setLength(500);
        offcut.setThickness(18);
        page.setRecords(List.of(offcut));
        page.setTotal(1);
        when(offcutService.page(any())).thenReturn(page);

        mockMvc.perform(get("/remnants")
                        .header("Authorization", "Bearer " + token())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].offcutId").value(1));
    }

    @Test
    void orderCreateEndpointReturnsCreatedOrder() throws Exception {
        TOrderVO orderVO = new TOrderVO();
        orderVO.setOrderId(1L);
        orderVO.setOrderNo("ORD202604300001");
        orderVO.setCustomerId(1L);
        orderVO.setProcessName("cabinet-door-cutting");
        when(orderService.createOrder(any())).thenReturn(orderVO);

        mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + token())
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
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(1));
    }

    @Test
    void layoutResultCreateEndpointReturnsSavedResult() throws Exception {
        TLayoutResult layoutResult = new TLayoutResult();
        layoutResult.setResultId(1L);
        layoutResult.setOrderId(1L);
        layoutResult.setUsageRate(new BigDecimal("0.8500"));
        layoutResult.setContainerCount(1);
        layoutResult.setResultJson("{}");
        when(layoutResultService.createResult(any())).thenReturn(layoutResult);

        mockMvc.perform(post("/layout-results")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "usageRate": 0.85,
                                  "containerCount": 1,
                                  "resultJson": "{}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resultId").value(1));
    }

    private String token() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("admin");
        return jwtUtil.generateToken(user);
    }
}
