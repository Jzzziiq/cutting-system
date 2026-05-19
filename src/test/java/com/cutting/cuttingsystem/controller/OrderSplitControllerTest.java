package com.cutting.cuttingsystem.controller;

import com.cutting.cuttingsystem.entitys.VO.SplitConfirmResultVO;
import com.cutting.cuttingsystem.entitys.VO.SplitItemVO;
import com.cutting.cuttingsystem.service.OrderSplitService;
import com.cutting.cuttingsystem.entitys.TUser;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderSplitControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @MockitoBean private OrderSplitService orderSplitService;

    private String bearerToken() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("operator");
        return "Bearer " + jwtUtil.generateToken(user, List.of("admin"));
    }

    @Test
    void executeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/order-split/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cabinetJson\":{\"cabinet\":{},\"boards\":[]}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void executeWithValidRequestReturnsSplitItems() throws Exception {
        SplitItemVO item = new SplitItemVO();
        item.setPartCode("ZG-001"); item.setPartName("左侧板"); item.setLength(2198); item.setWidth(600); item.setThickness(18);
        when(orderSplitService.execute(any())).thenReturn(List.of(item));

        String body = """
            {"cabinetJson":{"cabinet":{"name":"衣柜","room":"主卧"},"boards":[
              {"id":"b-001","type":"side","boardId":1,"designLength":2200,"designWidth":600,"thickness":18,
               "edgeBanding":{"left":false,"right":false,"top":true,"bottom":true},"hingeHoles":[]}
            ]}}
            """;
        mockMvc.perform(post("/order-split/execute")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].partCode").value("ZG-001"));
    }

    @Test
    void executeWithMissingCabinetJsonReturnsError() throws Exception {
        mockMvc.perform(post("/order-split/execute")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void confirmWithValidRequestReturnsResult() throws Exception {
        SplitConfirmResultVO result = new SplitConfirmResultVO();
        result.setOrderId(1L); result.setSplitBatchCode("ZG-20260519-001");
        result.setCreatedItemIds(List.of(1L, 2L));
        when(orderSplitService.confirm(any())).thenReturn(result);

        String body = """
            {"orderId":1,"confirmMode":"append",
             "cabinetJson":{"cabinet":{"name":"衣柜"},"boards":[
               {"id":"b-001","type":"side","boardId":1,"designLength":2200,"designWidth":600,"thickness":18,
                "edgeBanding":{"left":false,"right":false,"top":true,"bottom":true},"hingeHoles":[]}
             ]}}
            """;
        mockMvc.perform(post("/order-split/confirm")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.splitBatchCode").value("ZG-20260519-001"));
    }

    @Test
    void confirmWithoutOrderIdReturnsError() throws Exception {
        String body = """
            {"confirmMode":"append",
             "cabinetJson":{"cabinet":{"name":"衣柜"},"boards":[]}}
            """;
        mockMvc.perform(post("/order-split/confirm")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
