package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.CabinetTemplate;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.CabinetTemplateService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CabinetTemplateControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @MockitoBean private CabinetTemplateService cabinetTemplateService;

    private String bearerToken() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("operator");
        return "Bearer " + jwtUtil.generateToken(user, List.of("admin"));
    }

    @Test
    void pageReturnsTemplates() throws Exception {
        CabinetTemplate t = new CabinetTemplate();
        t.setId(1L); t.setName("衣柜"); t.setCategory("wardrobe"); t.setIsOfficial(1);
        Page<CabinetTemplate> page = new Page<>(1, 10);
        page.setRecords(List.of(t)); page.setTotal(1);
        when(cabinetTemplateService.pageQuery(any(), isNull())).thenReturn(page);

        mockMvc.perform(get("/cabinet-templates")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].name").value("衣柜"));
    }

    @Test
    void getByIdReturnsTemplate() throws Exception {
        CabinetTemplate t = new CabinetTemplate();
        t.setId(1L); t.setName("衣柜"); t.setCategory("wardrobe"); t.setIsOfficial(1);
        when(cabinetTemplateService.getTemplateById(1L)).thenReturn(t);

        mockMvc.perform(get("/cabinet-templates/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("衣柜"));
    }

    @Test
    void createTemplateSavesSuccessfully() throws Exception {
        CabinetTemplate created = new CabinetTemplate();
        created.setId(1L); created.setName("我的模板"); created.setCategory("wardrobe"); created.setIsOfficial(0);
        when(cabinetTemplateService.createTemplate(any())).thenReturn(created);

        mockMvc.perform(post("/cabinet-templates")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"我的模板\",\"category\":\"wardrobe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("我的模板"));
    }
}
