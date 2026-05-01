package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TOffcut;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TOffcutService;
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
class OffcutModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TOffcutService offcutService;

    @Test
    void pageReturnsOffcutRecords() throws Exception {
        Page<TOffcut> page = new Page<>(1, 10);
        page.setRecords(List.of(offcut(1L)));
        page.setTotal(1);
        when(offcutService.page(any())).thenReturn(page);

        mockMvc.perform(get("/remnants")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].offcutId").value(1))
                .andExpect(jsonPath("$.data.records[0].width").value(300));
    }

    @Test
    void pageAllowsMaximumPageSize() throws Exception {
        when(offcutService.page(any())).thenReturn(new Page<TOffcut>(1, 100));

        mockMvc.perform(get("/remnants")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void pageRejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/remnants")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }

    @Test
    void getByIdReturnsOffcutDetail() throws Exception {
        when(offcutService.getById(1L)).thenReturn(offcut(1L));

        mockMvc.perform(get("/remnants/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.offcutId").value(1))
                .andExpect(jsonPath("$.data.length").value(500));
    }

    @Test
    void getByIdReturnsBusinessErrorWhenOffcutDoesNotExist() throws Exception {
        when(offcutService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/remnants/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("remnant not found"));
    }

    @Test
    void getByIdRejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/remnants/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createOffcutReturnsCreatedOffcutWithDefaultsWhenRequestIsValid() throws Exception {
        when(offcutService.save(any(TOffcut.class))).thenReturn(true);

        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOffcutJsonWithoutDefaults()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.boardId").value(1))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.isEnabled").value(1));
    }

    @Test
    void createOffcutReturnsBusinessErrorWhenSaveFails() throws Exception {
        when(offcutService.save(any(TOffcut.class))).thenReturn(false);

        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOffcutJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("add remnant failed"));
    }

    @Test
    void createOffcutRejectsMissingBoardId() throws Exception {
        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.boardId").exists());
    }

    @Test
    void createOffcutRejectsZeroWidth() throws Exception {
        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardId": 1,
                                  "width": 0,
                                  "length": 500,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.width").exists());
    }

    @Test
    void createOffcutRejectsInvalidSourceOrderId() throws Exception {
        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardId": 1,
                                  "sourceOrderId": 0,
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sourceOrderId").exists());
    }

    @Test
    void createOffcutRejectsInvalidStatus() throws Exception {
        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardId": 1,
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "status": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").exists());
    }

    @Test
    void createOffcutRejectsOverlongBrand() throws Exception {
        String overlongBrand = "A".repeat(51);

        mockMvc.perform(post("/remnants")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardId": 1,
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "brand": "%s"
                                }
                                """.formatted(overlongBrand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.brand").exists());
    }

    @Test
    void updateOffcutReturnsSuccessWhenRequestIsValid() throws Exception {
        when(offcutService.updateById(any(TOffcut.class))).thenReturn(true);

        mockMvc.perform(put("/remnants/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOffcutJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateOffcutReturnsBusinessErrorWhenUpdateFails() throws Exception {
        when(offcutService.updateById(any(TOffcut.class))).thenReturn(false);

        mockMvc.perform(put("/remnants/999")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOffcutJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("update remnant failed"));
    }

    @Test
    void updateOffcutRejectsInvalidEnabledFlag() throws Exception {
        mockMvc.perform(put("/remnants/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardId": 1,
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "isEnabled": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.isEnabled").exists());
    }

    @Test
    void updateOffcutRejectsOverlongRemark() throws Exception {
        String overlongRemark = "A".repeat(256);

        mockMvc.perform(put("/remnants/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardId": 1,
                                  "width": 300,
                                  "length": 500,
                                  "thickness": 18,
                                  "remark": "%s"
                                }
                                """.formatted(overlongRemark)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.remark").exists());
    }

    @Test
    void deleteOffcutReturnsSuccessWhenRemoveSucceeds() throws Exception {
        when(offcutService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/remnants/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteOffcutReturnsBusinessErrorWhenRemoveFails() throws Exception {
        when(offcutService.removeById(999L)).thenReturn(false);

        mockMvc.perform(delete("/remnants/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("delete remnant failed"));
    }

    @Test
    void protectedOffcutEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/remnants")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    private TOffcut offcut(Long id) {
        TOffcut offcut = new TOffcut();
        offcut.setOffcutId(id);
        offcut.setBoardId(1L);
        offcut.setSourceOrderId(1L);
        offcut.setWidth(300);
        offcut.setLength(500);
        offcut.setThickness(18);
        offcut.setMaterialType("particle");
        offcut.setBrand("EGGER");
        offcut.setColor("white");
        offcut.setStatus(0);
        offcut.setIsEnabled(1);
        return offcut;
    }

    private String validOffcutJson() {
        return """
                {
                  "boardId": 1,
                  "sourceOrderId": 1,
                  "width": 300,
                  "length": 500,
                  "thickness": 18,
                  "materialType": "particle",
                  "brand": "EGGER",
                  "color": "white",
                  "status": 0,
                  "isEnabled": 1,
                  "remark": "usable remnant"
                }
                """;
    }

    private String validOffcutJsonWithoutDefaults() {
        return """
                {
                  "boardId": 1,
                  "sourceOrderId": 1,
                  "width": 300,
                  "length": 500,
                  "thickness": 18,
                  "materialType": "particle",
                  "brand": "EGGER",
                  "color": "white"
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
