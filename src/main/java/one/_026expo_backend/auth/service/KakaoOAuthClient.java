package one._026expo_backend.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
// 카카오 OAuth 토큰 교환과 프로필 조회를 담당하는 외부 연동 클라이언트
public class KakaoOAuthClient {

    private static final URI TOKEN_URI = URI.create("https://kauth.kakao.com/oauth/token");  // 인가 코드를 액세스 토큰으로 바꾸는 카카오 토큰 엔드포인트
    private static final URI USER_INFO_URI = URI.create("https://kapi.kakao.com/v2/user/me");  // 액세스 토큰으로 사용자 정보를 조회하는 카카오 프로필 엔드포인트

    private final HttpClient httpClient = HttpClient.newHttpClient(); // 외부 HTTP 요청을 보낼 기본 클라이언트
    private final ObjectMapper objectMapper; // 카카오 응답 JSON을 읽기 위한 Jackson 객체

    @Value("${oauth.kakao.client-id}")
    private String clientId;    // 카카오 개발자 콘솔의 REST API 키

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;   // 앱 설정에 따라 없을 수도 있는 선택 값

    public KakaoOAuthClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // authorization code를 이용해 카카오 액세스 토큰을 먼저 발급
    public KakaoProfile fetchProfile(String code, String redirectUri) {
        String accessToken = requestAccessToken(code, redirectUri);
        return requestProfile(accessToken);
    }

    // 카카오 토큰 API 호출에 필요한 form 데이터를 구성
    private String requestAccessToken(String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code"); // 인가 코드 방식으로 토큰을 받는다는 뜻
        form.put("client_id", clientId);
        form.put("redirect_uri", redirectUri);
        form.put("code", code);
        // client_secret이 설정된 앱이면 함께 전달
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.put("client_secret", clientSecret);
        }

        // 카카오 토큰 엔드포인트로 POST 요청
        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form)))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Kakao 토큰 엔드포인트 응답 비정상");
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        JsonNode body = readJson(response.body());
        String accessToken = body.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        return accessToken;
    }

    // 액세스 토큰으로 카카오 사용자 정보를 조회한 뒤, 필요한 값만 골라 반환한다
    private KakaoProfile requestProfile(String accessToken) {
        // 발급받은 액세스 토큰으로 사용자 정보를 요청한다
        HttpRequest request = HttpRequest.newBuilder(USER_INFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Kakao 프로필 엔드포인트 응답 비정상");
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        JsonNode body = readJson(response.body());

        // body에서 값 꺼내 필요한 요소 저장
        String providerId = body.path("id").asText(null);
        String email = body.path("kakao_account").path("email").asText(null);
        String nickname = body.path("kakao_account").path("profile").path("nickname").asText(null);

        if (nickname == null || nickname.isBlank()) {      // 닉네임이 비어있을 때
            nickname = body.path("properties").path("nickname").asText(null);
        }

        if (providerId == null || providerId.isBlank()) {   // 소셜 식별 아이디가 비어있을 때
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }

        return new KakaoProfile(providerId, email, nickname);
    }

    // 실제 HTTP 요청과 예외 처리는 한 곳에서 공통으로 처리한다
    private HttpResponse<String> send(HttpRequest request) {
        try {
            // 실제 HTTP 통신은 여기서 한 번에 처리
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return resp;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    // 카카오 응답 문자열을 JSON 트리로 바꿔서 읽는다
    // 응답 구조가 중첩되어있고 email이나 nickname이 없을 수도 있기 때문
    private JsonNode readJson(String body) {
        try {
            // 카카오 응답 문자열을 JSON으로 변환한다
            return objectMapper.readTree(body);
        } catch (IOException e) {
            log.error("카카오 응답 JSON 파싱 실패");
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    // field=value&field=value 형태로 form body를 만든다 (카카오 토큰 API가 요구하는 형식)
    private String toFormData(Map<String, String> form) {
        // form 값을 application/x-www-form-urlencoded 형태로 합친다
        return form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    // URL 전송용으로 인코딩한다
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // 카카오 사용자 식별자와 회원 가입에 필요한 정보만 담는 응답용 record
    public record KakaoProfile(String providerId, String email, String nickname) {
    }
}