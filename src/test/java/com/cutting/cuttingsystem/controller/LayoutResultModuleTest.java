package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TLayoutResultService;
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
class LayoutResultModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TLayoutResultService layoutResultService;

    @Test
    void pageReturnsLayoutResultRecords() throws Exception {
        Page<TLayoutResult> page = new Page<>(1, 10);
        page.setRecords(List.of(layoutResult(1L)));
        page.setTotal(1);
        when(layoutResultService.page(any())).thenReturn(page);

        mockMvc.perform(get("/layout-results")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].resultId").value(1))
                .andExpect(jsonPath("$.data.records[0].containerCount").value(1));
    }

    @Test
    void pageAllowsMaximumPageSize() throws Exception {
        when(layoutResultService.page(any())).thenReturn(new Page<TLayoutResult>(1, 100));

        mockMvc.perform(get("/layout-results")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void pageRejectsPageNumBelowMinimum() throws Exception {
        mockMvc.perform(get("/layout-results")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").exists());
    }

    @Test
    void listByOrderIdReturnsLayoutResults() throws Exception {
        when(layoutResultService.list(org.mockito.ArgumentMatchers.<Wrapper<TLayoutResult>>any()))
                .thenReturn(List.of(layoutResult(2L), layoutResult(1L)));

        mockMvc.perform(get("/layout-results/order/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].resultId").value(2))
                .andExpect(jsonPath("$.data[1].resultId").value(1));
    }

    @Test
    void listByOrderIdRejectsNonPositiveOrderId() throws Exception {
        mockMvc.perform(get("/layout-results/order/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").exists());
    }

    @Test
    void getByIdReturnsLayoutResultDetail() throws Exception {
        when(layoutResultService.getById(1L)).thenReturn(layoutResult(1L));

        mockMvc.perform(get("/layout-results/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resultId").value(1))
                .andExpect(jsonPath("$.data.resultJson").value("{\"containers\":1}"));
    }

    @Test
    void getByIdReturnsBusinessErrorWhenLayoutResultDoesNotExist() throws Exception {
        when(layoutResultService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/layout-results/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("layout result not found"));
    }

    @Test
    void getByIdRejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/layout-results/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createLayoutResultReturnsSavedResultWhenRequestIsValid() throws Exception {
        when(layoutResultService.createResult(any())).thenReturn(layoutResult(1L));

        mockMvc.perform(post("/layout-results")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLayoutResultJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resultId").value(1))
                .andExpect(jsonPath("$.data.usageRate").value(0.85));
    }

    @Test
    void createLayoutResultRejectsMissingOrderId() throws Exception {
        mockMvc.perform(post("/layout-results")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usageRate": 0.85,
                                  "containerCount": 1,
                                  "resultJson": "{}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").exists());
    }

    @Test
    void createLayoutResultRejectsUsageRateAboveMaximum() throws Exception {
        mockMvc.perform(post("/layout-results")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "usageRate": 1.01,
                                  "containerCount": 1,
                                  "resultJson": "{}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.usageRate").exists());
    }

    @Test
    void createLayoutResultRejectsNegativeTotalArea() throws Exception {
        mockMvc.perform(post("/layout-results")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "usageRate": 0.85,
                                  "totalArea": -1,
                                  "containerCount": 1,
                                  "resultJson": "{}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalArea").exists());
    }

    @Test
    void createLayoutResultRejectsBlankResultJson() throws Exception {
        mockMvc.perform(post("/layout-results")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "usageRate": 0.85,
                                  "containerCount": 1,
                                  "resultJson": " "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.resultJson").exists());
    }

    @Test
    void createLayoutResultRejectsOverlongImagePath() throws Exception {
        String overlongImagePath = "A".repeat(501);

        mockMvc.perform(post("/layout-results")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "usageRate": 0.85,
                                  "containerCount": 1,
                                  "resultJson": "{}",
                                  "imagePath": "%s"
                                }
                                """.formatted(overlongImagePath)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.imagePath").exists());
    }

    @Test
    void updateLayoutResultReturnsSuccessWhenRequestIsValid() throws Exception {
        when(layoutResultService.updateResult(eq(1L), any())).thenReturn(true);

        mockMvc.perform(put("/layout-results/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLayoutResultJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateLayoutResultReturnsBusinessErrorWhenUpdateFails() throws Exception {
        when(layoutResultService.updateResult(eq(999L), any())).thenReturn(false);

        mockMvc.perform(put("/layout-results/999")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLayoutResultJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("update layout result failed"));
    }

    @Test
    void updateLayoutResultRejectsNegativeContainerCount() throws Exception {
        mockMvc.perform(put("/layout-results/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 1,
                                  "usageRate": 0.85,
                                  "containerCount": -1,
                                  "resultJson": "{}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.containerCount").exists());
    }

    @Test
    void deleteLayoutResultReturnsSuccessWhenRemoveSucceeds() throws Exception {
        when(layoutResultService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/layout-results/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteLayoutResultReturnsBusinessErrorWhenRemoveFails() throws Exception {
        when(layoutResultService.removeById(999L)).thenReturn(false);

        mockMvc.perform(delete("/layout-results/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("delete layout result failed"));
    }

    @Test
    void protectedLayoutResultEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/layout-results")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    private TLayoutResult layoutResult(Long id) {
        TLayoutResult layoutResult = new TLayoutResult();
        layoutResult.setResultId(id);
        layoutResult.setOrderId(1L);
        layoutResult.setUsageRate(new BigDecimal("0.85"));
        layoutResult.setTotalArea(new BigDecimal("150000"));
        layoutResult.setContainerCount(1);
        layoutResult.setResultJson("{\"containers\":1}");
        layoutResult.setImagePath("/outputs/layout-1.png");
        layoutResult.setNcFilePath("/outputs/layout-1.nc");
        layoutResult.setLabelFilePath("/outputs/layout-1-label.pdf");
        return layoutResult;
    }

    private String validLayoutResultJson() {
        return """
                {
                  "orderId": 1,
                  "usageRate": 0.85,
                  "totalArea": 150000,
                  "containerCount": 1,
                  "resultJson": "{\\"containers\\":1}",
                  "imagePath": "/outputs/layout-1.png",
                  "ncFilePath": "/outputs/layout-1.nc",
                  "labelFilePath": "/outputs/layout-1-label.pdf"
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
