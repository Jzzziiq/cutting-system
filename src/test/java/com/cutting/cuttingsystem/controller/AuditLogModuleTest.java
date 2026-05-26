package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.VO.TAuditLogVO;
import com.cutting.cuttingsystem.mapper.TAuditLogMapper;
import com.cutting.cuttingsystem.mapper.TPermissionMapper;
import com.cutting.cuttingsystem.util.JwtUtil;
import com.cutting.cuttingsystem.entitys.TUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TAuditLogMapper auditLogMapper;

    @MockitoBean
    private TPermissionMapper permissionMapper;

    private String adminToken() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("admin");
        return "Bearer " + jwtUtil.generateToken(user, List.of("admin"));
    }

    // ──────── 查询接口 ────────

    @Test
    void listReturnsPage() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        Page<TAuditLogVO> emptyPage = new Page<>(1, 10, 0);
        when(auditLogMapper.selectLogPage(any(), isNull(), isNull(), isNull()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/audit-logs")
                        .header("Authorization", adminToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void listSupportsFilters() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        Page<TAuditLogVO> emptyPage = new Page<>(1, 10, 0);
        when(auditLogMapper.selectLogPage(any(), any(), any(), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/audit-logs")
                        .header("Authorization", adminToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("module", "客户管理")
                        .param("userId", "1")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void nonAdminCannotAccessAuditLogs() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("operator")))
                .thenReturn(List.of("customer:read"));

        TUser user = new TUser();
        user.setUserId(2L);
        user.setUsername("zhangsan");
        String operatorToken = "Bearer " + jwtUtil.generateToken(user, List.of("operator"));

        mockMvc.perform(get("/audit-logs")
                        .header("Authorization", operatorToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }
}
