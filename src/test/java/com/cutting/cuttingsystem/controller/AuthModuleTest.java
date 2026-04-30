package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.LoginInfo;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TUser;
import com.cutting.cuttingsystem.service.TBoardService;
import com.cutting.cuttingsystem.service.TUserService;
import com.cutting.cuttingsystem.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthModuleTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private TUserService tUserService;

    @MockitoBean
    private TBoardService tBoardService;

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() throws Exception {
        when(tUserService.login("admin", "123456"))
                .thenReturn(new LoginInfo(1L, "admin", "admin", "mock-token"));

        mockMvc.perform(post("/auth/login")
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.token").value("mock-token"));
    }

    @Test
    void loginReturnsBusinessErrorWhenCredentialsAreInvalid() throws Exception {
        when(tUserService.login("admin", "wrong-password")).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .param("username", "admin")
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void loginRejectsMissingUsername() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").exists());
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .param("username", "admin")
                        .param("password", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void registerReturnsSuccessWhenUsernameIsAvailable() throws Exception {
        when(tUserService.register("new-user", "123456")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .param("username", "new-user")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"));
    }

    @Test
    void registerReturnsFailWhenUsernameExists() throws Exception {
        when(tUserService.register("admin", "123456")).thenReturn(false);

        mockMvc.perform(post("/auth/register")
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(100));
    }

    @Test
    void registerRejectsMissingPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void logoutReturnsSuccessWithoutTokenInCurrentConfiguration() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("success"));
    }

    @Test
    void protectedEndpointRejectsWrongAuthorizationScheme() throws Exception {
        mockMvc.perform(get("/boards")
                        .header("Authorization", "Token " + token())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAllowsValidBearerToken() throws Exception {
        when(tBoardService.page(any())).thenReturn(new Page<TBoard>(1, 10));

        mockMvc.perform(get("/boards")
                        .header("Authorization", "Bearer " + token())
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private String token() {
        TUser user = new TUser();
        user.setUserId(1L);
        user.setUsername("admin");
        return jwtUtil.generateToken(user);
    }
}
