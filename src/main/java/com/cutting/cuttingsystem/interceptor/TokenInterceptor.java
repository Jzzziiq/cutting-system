package com.cutting.cuttingsystem.interceptor;

import com.cutting.cuttingsystem.util.JwtUtil;
import com.cutting.cuttingsystem.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("preHandle...");

        // 1. 从标准请求头 Authorization 中获取令牌
        String authHeader = request.getHeader("Authorization");

        // 2. 检查 Authorization 头是否存在且格式正确（以 "Bearer " 开头）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("令牌缺失或格式错误，响应 401");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3. 提取实际的 token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(7); // "Bearer " 长度为 7

        // 4. 验证 token
        try {
            if (!jwtUtil.validateToken(token)) {
                log.info("令牌非法，响应 401");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        } catch (Exception e) {
            log.info("令牌非法，响应 401");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 5. 从 token 中获取 userId 并设置到线程上下文
        Long userId = jwtUtil.getUserIdFromToken(token);
        UserContext.setCurrentUserId(userId);
        
        log.info("令牌合法，userId: {}", userId);
        return true;
    }

    // 请求访问完资源后 处理
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        log.info("postHandle");
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 6. 清除线程上下文，防止内存泄漏
        UserContext.clear();
        log.info("afterCompletion, 已清除用户上下文");
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
