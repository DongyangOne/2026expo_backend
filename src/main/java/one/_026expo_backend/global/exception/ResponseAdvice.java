package one._026expo_backend.global.exception;

import one._026expo_backend.global.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 응답 객체 자동 래핑 처리기
 * * 컨트롤러에서 반환하는 순수 데이터를 공통 응답 규격(ApiResponse)으로 자동 감싸기 위해 사용함
 * * 중복 코드를 줄이고 모든 API 응답의 일관성을 강제함
 */
@RestControllerAdvice(basePackages = "mars.tripplanappbackend")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 적용 여부 결정
     * * 이미 ApiResponse 타입이거나, Swagger 관련 응답 등 특수 케이스는 래핑에서 제외함
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 반환 타입이 이미 ApiResponse인 경우 중복 래핑을 방지하기 위해 false 반환
        return !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    /**
     * 실제 응답 값 변환 로직
     * * 컨트롤러 리턴 직전에 실행되어 최종 JSON 구조를 결정함
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 이미 ApiResponse로 감싸져 있거나(Error 처리 등),
        // ResponseEntity(직접 응답 제어)인 경우 원본 그대로 반환
        if (body instanceof ApiResponse || body instanceof ResponseEntity<?>) {
            return body;
        }

        // String 타입의 경우 MessageConverter 우선순위 문제로 인해
        // 단순 래핑 시 ClassCastException이 발생할 위험이 있으므로 주의 필요 (필요 시 별도 처리)

        // 결과값이 없는 경우에도 공통 규격의 틀을 유지하기 위해 빈 데이터로 응답함
        if (body == null) {
            return ApiResponse.ok(null);
        }

        // 그 외 모든 순수 객체(DTO, List 등)는 ApiResponse.ok()로 감싸서 반환
        return ApiResponse.ok(body);
    }
}