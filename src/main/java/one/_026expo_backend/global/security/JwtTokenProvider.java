package one._026expo_backend.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 * * 사용자 인증 후 accessToken 발급
 * * 이후 API 호출 시 토큰 검증에 사용
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecretString;

    @Value("${jwt.access-expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    // tokenType 상수 설정
    private final String ACCESS = "ACCESS";
    private final String REFRESH = "REFRESH";

    /**
     * JWT Secret 키 생성 (String -> SecretKey)
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * userId와 role 기반 AccessToken 생성
     */
    public String createAccessToken(Long userId, Role role) {
        return createToken(userId, role, ACCESS, accessExpirationMs);
    }

    /**
     * userId와 role 기반 RefreshToken 생성
     */
    public String createRefreshToken(Long userId, Role role) {
        return createToken(userId, role, REFRESH, refreshExpirationMs);
    }

    /**
     * 토큰 생성 공통 로직
     * userId를 기반으로 생성하고 role 정보를 포함
     */
    private String createToken(Long userId, Role role, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("token_type", tokenType)
                .claim("role", role.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰에서 userId 추출 실패", e);
            return null;
        }
    }

    /**
     * 토큰에서 Role 추출
     */
    public Role getRole(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            String roleString = claims.get("role", String.class);
            if (roleString != null) {
                return Role.valueOf(roleString);
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰에서 role 추출 실패", e);
            return null;
        }
    }

    /**
     * 토큰에서 토큰 타입 추출 (ACCESS 또는 REFRESH)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("token_type", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰에서 토큰 타입 추출 실패", e);
            return null;
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public UseYnEnum validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token);
            return UseYnEnum.Y;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰 유효성 검증 실패", e);
            return UseYnEnum.N;
        }
    }

    /**
     * 토큰의 만료 시간 조회
     */
    public Date getTokenExpirationTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰에서 만료 시간 조회 실패", e);
            return null;
        }
    }

}
