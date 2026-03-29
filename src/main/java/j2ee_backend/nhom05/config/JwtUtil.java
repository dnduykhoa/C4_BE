package j2ee_backend.nhom05.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private static final String SESSION_ID_CLAIM = "sid";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.remember-me-expiration}")
    private long rememberMeExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Tạo token với thời hạn theo rememberMe
    public String generateToken(String username, boolean rememberMe) {
        return generateToken(username, rememberMe, null);
    }

    public String generateToken(String username, boolean rememberMe, Long sessionId) {
        long exp = rememberMe ? rememberMeExpiration : expiration;
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + exp))
                .signWith(getSigningKey());

        if (sessionId != null) {
            builder.claim(SESSION_ID_CLAIM, sessionId);
        }

        return builder.compact();
    }

    // Lấy username từ token
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractSessionId(String token) {
        Object value = parseClaims(token).get(SESSION_ID_CLAIM);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Kiểm tra token còn hợp lệ không
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
