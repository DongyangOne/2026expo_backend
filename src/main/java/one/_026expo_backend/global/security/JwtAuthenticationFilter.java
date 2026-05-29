package one._026expo_backend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
        } catch (Exception e) {
            // 토큰 검증 중 예외 발생해도 다음 필터 진행 (요청 끊김 방지)
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
