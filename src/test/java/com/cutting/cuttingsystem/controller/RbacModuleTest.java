package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TRole;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.mapper.TPermissionMapper;
import com.cutting.cuttingsystem.service.TRoleService;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacModuleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TUserService userService;

    @MockitoBean
    private TRoleService roleService;

    @MockitoBean
    private TPermissionMapper permissionMapper;

    private String adminToken() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("admin");
        return "Bearer " + jwtUtil.generateToken(user, List.of("admin"));
    }

    private String viewerToken() {
        TUser user = new TUser();
        user.setUserId(2L);
        user.setUsername("viewer");
        return "Bearer " + jwtUtil.generateToken(user, List.of("viewer"));
    }

    // ──────────────────── 鉴权测试 ────────────────────

    @Test
    void adminCanAccessUserManagement() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage", "customer:read"));
        when(userService.page(any(), any()))
                .thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/users")
                        .header("Authorization", adminToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void viewerCannotAccessUserManagement() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("viewer")))
                .thenReturn(List.of("customer:read", "layout:read"));

        mockMvc.perform(get("/users")
                        .header("Authorization", viewerToken())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanAccessAlgorithm() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage", "algorithm:execute"));

        mockMvc.perform(post("/algorithm/answer")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"L\":1000,\"W\":500,\"rotateEnable\":true,\"gapDistance\":0,\"squareList\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerCannotAccessAlgorithm() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("viewer")))
                .thenReturn(List.of("customer:read", "layout:read"));

        mockMvc.perform(post("/algorithm/answer")
                        .header("Authorization", viewerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void missingAuthHeaderReturns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────── 角色列表测试 ────────────────────

    @Test
    void listRolesIsAccessibleToAuthenticatedUsers() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        when(roleService.listAllRoles()).thenReturn(List.of());

        // /users/roles has @RequirePermission({}) — empty = any authenticated user
        mockMvc.perform(get("/users/roles")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ──────────────────── 用户管理 CRUD 测试 ────────────────────

    @Test
    void getUserDetailReturns404WhenUserNotFound() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        when(userService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/users/999")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("用户不存在"));
    }

    @Test
    void assignRolesReturnsErrorWhenUserNotFound() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        when(userService.exists(any())).thenReturn(false);

        mockMvc.perform(put("/users/roles")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"roleIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void assignRolesSucceedsWhenUserExists() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        when(userService.exists(any())).thenReturn(true);

        mockMvc.perform(put("/users/roles")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"roleIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateUserStatusReturnsErrorWhenUserNotFound() throws Exception {
        when(permissionMapper.selectPermCodesByRoleCodes(List.of("admin")))
                .thenReturn(List.of("user:manage"));
        when(userService.getById(999L)).thenReturn(null);

        mockMvc.perform(put("/users/999/status")
                        .header("Authorization", adminToken())
                        .param("accountStatus", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
