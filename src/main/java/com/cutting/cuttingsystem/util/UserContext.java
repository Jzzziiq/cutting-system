package com.cutting.cuttingsystem.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户上下文工具类 - 存储当前登录用户的 userId / roles / permissions
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_TL = new ThreadLocal<>();
    private static final ThreadLocal<Long> ORG_ID_TL = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> ROLES_TL = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<String>> PERMISSIONS_TL = ThreadLocal.withInitial(ArrayList::new);

    public static void setCurrentUserId(Long userId) {
        USER_ID_TL.set(userId);
    }

    public static Long getCurrentUserId() {
        return USER_ID_TL.get();
    }

    public static void setCurrentOrgId(Long orgId) {
        ORG_ID_TL.set(orgId);
    }

    public static Long getCurrentOrgId() {
        return ORG_ID_TL.get();
    }

    public static void setRoles(List<String> roles) {
        ROLES_TL.set(roles != null ? roles : new ArrayList<>());
    }

    public static List<String> getRoles() {
        return ROLES_TL.get() != null ? ROLES_TL.get() : Collections.emptyList();
    }

    public static void setPermissions(List<String> permissions) {
        PERMISSIONS_TL.set(permissions != null ? permissions : new ArrayList<>());
    }

    public static List<String> getPermissions() {
        return PERMISSIONS_TL.get() != null ? PERMISSIONS_TL.get() : Collections.emptyList();
    }

    public static boolean hasPermission(String permCode) {
        List<String> perms = getPermissions();
        return perms.contains(permCode);
    }

    public static boolean hasAnyPermission(String... permCodes) {
        List<String> perms = getPermissions();
        for (String code : permCodes) {
            if (perms.contains(code)) return true;
        }
        return false;
    }

    public static void clear() {
        USER_ID_TL.remove();
        ORG_ID_TL.remove();
        ROLES_TL.remove();
        PERMISSIONS_TL.remove();
    }
}
