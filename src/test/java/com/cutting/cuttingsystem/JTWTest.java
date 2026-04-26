package com.cutting.cuttingsystem;

import com.cutting.cuttingsystem.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.util.Date;

@SpringBootTest
@Slf4j
public class JTWTest {
    @Autowired
    private SecretKey jwtSecretKey;
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    @Test
    public void testGenerateJWT(){

        String compact = Jwts.builder()
//                .claims(claims)
//                .subject(String.valueOf(user.getId()))
//                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(jwtSecretKey)
                .compact();
        log.info("Generated JWT: {}", compact);
    }
    /*
    eyJhbGciOiJIUzI1NiJ9
    .eyJleHAiOjE3NzM5OTExMDZ9
    .W71qeqeN3XeYGmfpKAKirr9Fp5gwAOIU4GnJMACMvig


     */
}
