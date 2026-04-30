package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TBoardService;
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
class BoardModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TBoardService tBoardService;

    @Test
    void pageReturnsBoardRecords() throws Exception {
        Page<TBoard> page = new Page<>(1, 10);
        page.setRecords(List.of(board(1L)));
        page.setTotal(1);
        when(tBoardService.page(any())).thenReturn(page);

        mockMvc.perform(get("/boards")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].boardId").value(1))
                .andExpect(jsonPath("$.data.records[0].brand").value("EGGER"));
    }

    @Test
    void pageAllowsMaximumPageSize() throws Exception {
        when(tBoardService.page(any())).thenReturn(new Page<TBoard>(1, 100));

        mockMvc.perform(get("/boards")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void pageRejectsPageNumBelowMinimum() throws Exception {
        mockMvc.perform(get("/boards")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").exists());
    }

    @Test
    void pageRejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/boards")
                        .header("Authorization", bearerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").exists());
    }

    @Test
    void getByIdReturnsBoardDetail() throws Exception {
        when(tBoardService.getById(1)).thenReturn(board(1L));

        mockMvc.perform(get("/boards/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.boardId").value(1))
                .andExpect(jsonPath("$.data.width").value(1220));
    }

    @Test
    void getByIdReturnsBusinessErrorWhenBoardDoesNotExist() throws Exception {
        when(tBoardService.getById(999)).thenReturn(null);

        mockMvc.perform(get("/boards/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("board not found"));
    }

    @Test
    void getByIdRejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/boards/0")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createBoardReturnsSuccessWhenRequestIsValid() throws Exception {
        when(tBoardService.save(any(TBoard.class))).thenReturn(true);

        mockMvc.perform(post("/boards")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBoardJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void createBoardRejectsMissingBrand() throws Exception {
        mockMvc.perform(post("/boards")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "materialType": "particle",
                                  "color": "white",
                                  "sizeType": "standard",
                                  "width": 1220,
                                  "length": 2440,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.brand").exists());
    }

    @Test
    void createBoardRejectsZeroWidth() throws Exception {
        mockMvc.perform(post("/boards")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "EGGER",
                                  "materialType": "particle",
                                  "color": "white",
                                  "sizeType": "standard",
                                  "width": 0,
                                  "length": 2440,
                                  "thickness": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.width").exists());
    }

    @Test
    void createBoardRejectsOverlongBrand() throws Exception {
        String overlongBrand = "A".repeat(51);

        mockMvc.perform(post("/boards")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "%s",
                                  "materialType": "particle",
                                  "color": "white",
                                  "sizeType": "standard",
                                  "width": 1220,
                                  "length": 2440,
                                  "thickness": 18
                                }
                                """.formatted(overlongBrand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.brand").exists());
    }

    @Test
    void updateBoardReturnsSuccessWhenRequestIsValid() throws Exception {
        when(tBoardService.updateById(any(TBoard.class))).thenReturn(true);

        mockMvc.perform(put("/boards/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBoardJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateBoardRejectsNegativeThickness() throws Exception {
        mockMvc.perform(put("/boards/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "EGGER",
                                  "materialType": "particle",
                                  "color": "white",
                                  "sizeType": "standard",
                                  "width": 1220,
                                  "length": 2440,
                                  "thickness": -1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.thickness").exists());
    }

    @Test
    void updateBoardRejectsInvalidEnabledFlag() throws Exception {
        mockMvc.perform(put("/boards/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brand": "EGGER",
                                  "materialType": "particle",
                                  "color": "white",
                                  "sizeType": "standard",
                                  "width": 1220,
                                  "length": 2440,
                                  "thickness": 18,
                                  "isEnabled": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.isEnabled").exists());
    }

    @Test
    void deleteBoardReturnsSuccessWhenRemoveSucceeds() throws Exception {
        when(tBoardService.removeById(1)).thenReturn(true);

        mockMvc.perform(delete("/boards/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deleteBoardReturnsBusinessErrorWhenRemoveFails() throws Exception {
        when(tBoardService.removeById(999)).thenReturn(false);

        mockMvc.perform(delete("/boards/999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void protectedBoardEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/boards")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    private TBoard board(Long id) {
        TBoard board = new TBoard();
        board.setBoardId(id);
        board.setBrand("EGGER");
        board.setMaterialType("particle");
        board.setColor("white");
        board.setSizeType("standard");
        board.setWidth(1220);
        board.setLength(2440);
        board.setThickness(18);
        board.setIsEnabled(1);
        return board;
    }

    private String validBoardJson() {
        return """
                {
                  "brand": "EGGER",
                  "materialType": "particle",
                  "color": "white",
                  "sizeType": "standard",
                  "width": 1220,
                  "length": 2440,
                  "thickness": 18,
                  "isEnabled": 1,
                  "remark": "main stock board"
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
