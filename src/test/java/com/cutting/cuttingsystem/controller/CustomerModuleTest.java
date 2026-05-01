package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TCustomerService;
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
class CustomerModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TCustomerService customerService;

    @Test
    void pageReturnsCustomerRecords() throws Exception {
        Page<TCustomer> page = new Page<>(1, 10);
        page.setRecords(List.of(customer(1L)));
        page.setTotal(1);
        when(customerService.page(any())).thenReturn(page);

        mockMvc.perform(get("/customers")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].customerId").value(1))
                .andExpect(jsonPath("$.data.records[0].customerName").value("ACME Furniture"));
    }

    @Test
    void pageAllowsMaximumPageSize() throws Exception {
        when(customerService.page(any())).thenReturn(new Page<TCustomer>(1, 100));

        mockMvc.perform(get("/customers")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void pageRejectsPageNumBelowMinimum() throws Exception {
        mockMvc.perform(get("/customers")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").exists());
    }

    @Test
    void pageRejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/customers")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }

    @Test
    void getByIdReturnsCustomerDetail() throws Exception {
        when(customerService.getById(1)).thenReturn(customer(1L));

        mockMvc.perform(get("/customers/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.customerId").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800000000"));
    }

    @Test
    void getByIdReturnsBusinessErrorWhenCustomerDoesNotExist() throws Exception {
        when(customerService.getById(999)).thenReturn(null);

        mockMvc.perform(get("/customers/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("customer not found"));
    }

    @Test
    void getByIdRejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/customers/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createCustomerReturnsSuccessWhenRequestIsValid() throws Exception {
        when(customerService.save(any(TCustomer.class))).thenReturn(true);

        mockMvc.perform(post("/customers")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void createCustomerRejectsMissingName() throws Exception {
        mockMvc.perform(post("/customers")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800000000",
                                  "address": "No.1 Industrial Road"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.customerName").exists());
    }

    @Test
    void createCustomerRejectsBlankPhone() throws Exception {
        mockMvc.perform(post("/customers")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "ACME Furniture",
                                  "phone": " ",
                                  "address": "No.1 Industrial Road"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void createCustomerRejectsOverlongPhone() throws Exception {
        mockMvc.perform(post("/customers")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "ACME Furniture",
                                  "phone": "123456789012345678901",
                                  "address": "No.1 Industrial Road"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void createCustomerRejectsOverlongAddress() throws Exception {
        String overlongAddress = "A".repeat(256);

        mockMvc.perform(post("/customers")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "ACME Furniture",
                                  "phone": "13800000000",
                                  "address": "%s"
                                }
                                """.formatted(overlongAddress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.address").exists());
    }

    @Test
    void updateCustomerReturnsSuccessWhenRequestIsValid() throws Exception {
        when(customerService.updateById(any(TCustomer.class))).thenReturn(true);

        mockMvc.perform(put("/customers/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateCustomerRejectsInvalidEnabledFlag() throws Exception {
        mockMvc.perform(put("/customers/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "ACME Furniture",
                                  "phone": "13800000000",
                                  "address": "No.1 Industrial Road",
                                  "isEnabled": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.isEnabled").exists());
    }

    @Test
    void updateCustomerRejectsMissingPhone() throws Exception {
        mockMvc.perform(put("/customers/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "ACME Furniture",
                                  "address": "No.1 Industrial Road"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void deleteCustomerReturnsSuccessWhenRemoveSucceeds() throws Exception {
        when(customerService.removeById(1)).thenReturn(true);

        mockMvc.perform(delete("/customers/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteCustomerReturnsBusinessErrorWhenRemoveFails() throws Exception {
        when(customerService.removeById(999)).thenReturn(false);

        mockMvc.perform(delete("/customers/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void protectedCustomerEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/customers")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    private TCustomer customer(Long id) {
        TCustomer customer = new TCustomer();
        customer.setCustomerId(id);
        customer.setCustomerName("ACME Furniture");
        customer.setPhone("13800000000");
        customer.setAddress("No.1 Industrial Road");
        customer.setIsEnabled(1);
        return customer;
    }

    private String validCustomerJson() {
        return """
                {
                  "customerName": "ACME Furniture",
                  "phone": "13800000000",
                  "address": "No.1 Industrial Road",
                  "isEnabled": 1,
                  "remark": "priority customer"
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
