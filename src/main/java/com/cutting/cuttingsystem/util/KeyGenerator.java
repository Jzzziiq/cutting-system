package com.cutting.cuttingsystem.util;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JWT 密钥生成器 - 生成用于 HS256 签名的安全密钥
 */
public class KeyGenerator {

    public static void main(String[] args) {
        byte[] keyBytes256 = generateSecureKey(32);
        SecretKey keyForHS256 = Keys.hmacShaKeyFor(keyBytes256);
        String base64KeyHS256 = Base64.getEncoder().encodeToString(keyForHS256.getEncoded());
        System.out.println("HS256 Key (Base64): " + base64KeyHS256);
    }

    /**
     * 生成安全的随机密钥字节数组
     * @param length 密钥长度（字节），HS256 至少需要 32 字节
     * @return 随机密钥字节数组
     */
    private static byte[] generateSecureKey(int length) {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[length];
        random.nextBytes(key);
        return key;
    }
}