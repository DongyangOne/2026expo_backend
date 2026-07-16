package one._026expo_backend.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.SocialProfileDto;
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
public class NaverOAuthClient {

    private static final URI TOKEN_URI = URI.create("https://nid.naver.com/oauth2.0/token");
    private static final URI USER_INFO_URI = URI.create("https://openapi.naver.com/v1/nid/me");

    private final HttpClient httpClient = HttpClient.newHttpClient();// 외부 HTTP 요청을 보낼 기본 클라이언트
    private final ObjectMapper objectMapper;// 응답 JSON을 읽기 위한 Jackson 객체

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret:}")
    private String clientSecret;

    @Value("${oauth.naver.state}")
    private String state;

    public NaverOAuthClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // authorization code를 이용해 네이버 액세스 토큰을 먼저 발급
    public SocialProfileDto fetchProfile(String code, String redirectUri) {
        String accessToken = requestAccessToken(code, redirectUri);
        return requestProfile(accessToken);
    }

    // 네이버 토큰 API 호출에 필요한 form 데이터를 구성
    private String requestAccessToken(String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code"); // 인가 코드 방식으로 토큰을 받는다는 뜻
        form.put("client_id", clientId);
        form.put("redirect_uri", redirectUri);
        form.put("state", state);
        form.put("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.put("client_secret", clientSecret);
        }

        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form)))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Naver 토큰 엔드포인트 응답 비정상: status={}, body={}", response.statusCode(), truncate(response.body()));
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        }

        JsonNode body = readJson(response.body());
        String accessToken = body.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Naver 엑세스 토큰 비어있음: response={}", truncate(response.body()));
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        }

        return accessToken;
    }

    private SocialProfileDto requestProfile(String accessToken) {
        // 사용자 정보 요청
        HttpRequest request = HttpRequest.newBuilder(USER_INFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Naver 프로필 엔드포인트 응답 비정상: status={}, body={}", response.statusCode(), truncate(response.body()));
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        }

        JsonNode body = readJson(response.body());
        JsonNode resp = body.path("response");
        String providerId = resp.path("id").asText(null);
        String name = resp.path("name").asText(null);
        String email = resp.path("email").asText(null);

        if (providerId == null || providerId.isBlank()) {
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        }

        return new SocialProfileDto(providerId, email, name);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        }
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            log.error("네이버 응답 JSON 파싱 실패: body={}", truncate(body), e);
            throw new BusinessException(ErrorCode.NAVER_LOGIN_FAILED);
        }
    }

    private String truncate(String body) {
        if (body == null) {
            return "null";
        }

        String trimmed = body.replaceAll("\\s+", " ").trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }

        return trimmed.substring(0, 500) + "...";
    }

    private String toFormData(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
