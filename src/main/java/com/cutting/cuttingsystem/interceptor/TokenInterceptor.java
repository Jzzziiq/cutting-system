package com.cutting.cuttingsystem.interceptor;

import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.exception.ForbiddenException;
import com.cutting.cuttingsystem.mapper.TPermissionMapper;
import com.cutting.cuttingsystem.util.JwtUtil;
import com.cutting.cuttingsystem.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TPermissionMapper permissionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        Long orgId = jwtUtil.getOrgIdFromToken(token);
        List<String> roles = jwtUtil.getRolesFromToken(token);
        List<String> permissions;
        try {
            permissions = permissionMapper.selectPermCodesByRoleCodes(roles);
        } catch (Exception e) {
            log.warn("权限表查询失败 (RBAC 迁移可能未执行), 回退为全量许可: {}", e.getMessage());
            permissions = List.of("*");
        }

        UserContext.setCurrentUserId(userId);
        UserContext.setCurrentOrgId(orgId);
        UserContext.setRoles(roles);
        UserContext.setPermissions(permissions);

        // 检查 @RequirePermission 注解
        if (handler instanceof HandlerMethod hm) {
            RequirePermission annotation = hm.getMethodAnnotation(RequirePermission.class);
            if (annotation == null) {
                annotation = hm.getBeanType().getAnnotation(RequirePermission.class);
            }
            if (annotation != null) {
                String[] required = annotation.value();
                if (required.length == 0) return true;
                // "*" 通配符表示 RBAC 表未就绪，跳过检查
                if (!permissions.contains("*")) {
                    boolean hasAny = Arrays.stream(required).anyMatch(permissions::contains);
                    if (!hasAny) {
                        log.info("权限不足, userId={}, required={}, userPerms={}", userId, required, permissions);
                        throw new ForbiddenException("权限不足");
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
