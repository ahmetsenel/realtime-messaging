package com.ahmetsenel.authservice.security;

import com.ahmetsenel.commonlib.exception.BusinessException;
import com.ahmetsenel.commonlib.exception.MessageType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.private-key}")
    private Resource privateKeyResource;

    @Value("${jwt.expiration}")
    private Long expiration;

    private PrivateKey privateKey;

    @PostConstruct
    public void init() throws Exception {
        String keyContent = new String(privateKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String privateKeyPEM = keyContent
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = kf.generatePrivate(spec);
    }

    public String generateToken(Long userId, String username) {
        try {
            return Jwts.builder()
                    .setSubject(String.valueOf(userId))
                    .claim("username", username)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(this.privateKey, SignatureAlgorithm.RS256)
                    .compact();
        } catch (Exception e) {
            log.error("An error occurred while generating the token: ", e);
            throw new BusinessException(MessageType.TOKEN_GENERATION_FAILED);
        }
    }
}