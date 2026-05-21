package one._026expo_backend.global.config.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 인증된 사용자 정보를 컨트롤러 파라미터에 주입하기 위한 커스텀 어노테이션입니다.
 *
 * @AuthenticationPrincipal을 감싸 SecurityContext에 저장된 UserPrincipal을 바인딩합니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {
}
