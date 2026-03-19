package com.cutting.cuttingsystem.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户上下文工具类 - 存储当前登录用户的 userId
 * 
 * <p>使用 ThreadLocal 实现线程隔离，确保每个请求的用户 ID 独立存储</p>
 * 
 * 使用场景：
 * 1. 在拦截器中解析 Token 后设置 userId
 * 2. 在 Service/Mapper 层获取当前登录用户 ID
 * 3. 配合 MyBatis-Plus 多租户插件实现数据隔离
 * 
 * 使用示例：
 * // 1. 在拦截器中设置（TokenInterceptor）
 * Long userId = jwtUtil.getUserIdFromToken(token);
 * UserContext.setCurrentUserId(userId);
 * 
 * // 2. 在 Service 层获取（新增数据时）
 * TBoard board = new TBoard();
 * board.setUserId(UserContext.getCurrentUserId());
 * this.save(board);
 * 
 * // 3. 在拦截器的 afterCompletion 中清除
 * @Override
 * public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
 *     UserContext.clear();
 * }
 * 
 * 注意事项：
 * 1. 必须在请求结束后调用 clear() 方法，防止内存泄漏
 * 2. ThreadLocal 保证同一线程内可共享 userId，无需参数传递
 * 3. 如果在异步线程中使用，需要手动传递 userId
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前用户的 ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID_THREAD_LOCAL.set(userId);
    }

    /**
     * 获取当前用户的 ID
     */
    public static Long getCurrentUserId() {
        return USER_ID_THREAD_LOCAL.get();
    }

    /**
     * 清除当前用户的 ID（请求结束后调用）
     */
    public static void clear() {
        USER_ID_THREAD_LOCAL.remove();
    }
}
