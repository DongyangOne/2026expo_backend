package one._026expo_backend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 토큰 검증 필터
 * * 모든 요청을 가로채서 Authorization 헤더의 JWT 토큰을 검증
 * * 유효한 토큰이면 SecurityContext에 사용자 정보를 저장하여 이후 @PreAuthorize 등에서 사용 가능하게 함
 */
@Slf4j  // logger 자동 주입을 위해 필요
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            // 토큰이 존재하고 유효하며 ACCESS 타입이면 인증 정보 설정
            if (token != null && jwtProvider.validateToken(token) == UseYnEnum.Y) {
                String tokenType = jwtProvider.getTokenType(token);
                
                // ACCESS 토큰만 API 요청에 사용 가능 (REFRESH 토큰 차단)
                if (tokenType != null && tokenType.equals("ACCESS")) {
                    Long userId = jwtProvider.getUserId(token);
                    Role role = jwtProvider.getRole(token);

                    // 역할 정보를 Spring Security 권한 접근용 GrantedAuthority로 변환
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (role != null) {
                        // Spring Security는 ROLE_ 접두사 권장
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
                    }

                    // Spring Security의 Authentication 객체 생성
                    // Principal: userId, Credentials: 토큰, Authorities: 사용자의 역할 정보
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            userId, token, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (IllegalArgumentException e) {
            // 토큰이 null이거나 빈 문자열인 경우
            log.debug("JWT 토큰이 null이거나 비어있습니다", e);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 토큰의 유효 기간이 만료된 경우
            log.debug("JWT 토큰이 만료되었습니다", e);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // 토큰 형식이 잘못된 경우 (손상)
            log.debug("JWT 토큰 형식이 올바르지 않습니다", e);
        } catch (io.jsonwebtoken.SignatureException e) {
            // 토큰 서명 검증에 실패한 경우 (변조)
            log.debug("JWT 토큰의 서명이 유효하지 않습니다", e);
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            // 지원하지 않는 JWT 형식인 경우
            log.debug("지원하지 않는 JWT 형식입니다", e);
        } catch (Exception e) {
            // 위의 예외가 아닌 다른 예외 발생 시
            log.warn("JWT 검증 중 예상치 못한 예외가 발생했습니다", e);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 "Bearer " 접두사를 제거하고 토큰만 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // "Bearer " 제거 
        }
        return null;
    }
}
