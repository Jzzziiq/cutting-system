package com.cutting.cuttingsystem.util;

import com.cutting.cuttingsystem.entitys.TUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    @Autowired
    private SecretKey jwtSecretKey;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 生成 JWT Token（含角色编码和组织ID）
     */
    public String generateToken(TUser user, List<String> roleCodes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        if (roleCodes != null && !roleCodes.isEmpty()) {
            claims.put("roles", roleCodes);
        }
        if (user.getOrgId() != null) {
            claims.put("orgId", user.getOrgId());
        }
        if (user.getOrgRole() != null) {
            claims.put("orgRole", user.getOrgRole());
        }

        return Jwts.builder()
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(jwtSecretKey)
                .compact();
    }

    /**
     * 从 Token 中获取 Claims，就是解析token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取组织ID
     */
    public Long getOrgIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("orgId", Long.class);
    }

    /**
     * 从 Token 中获取组织角色
     */
    public String getOrgRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("orgRole", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }
        } catch (Exception e) {
            // ignore
        }
        return Collections.emptyList();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
