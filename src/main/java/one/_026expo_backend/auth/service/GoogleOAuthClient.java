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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class GoogleOAuthClient {

    private static final URI TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
    private static final URI USER_INFO_URI = URI.create("https://www.googleapis.com/oauth2/v2/userinfo");

    private final HttpClient httpClient = HttpClient.newHttpClient();// 외부 HTTP 요청을 보낼 기본 클라이언트
    private final ObjectMapper objectMapper;// 응답 JSON을 읽기 위한 Jackson 객체

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret:}")
    private String clientSecret;

    public GoogleOAuthClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // authorization code를 이용해 구글 액세스 토큰을 먼저 발급
    public SocialProfileDto fetchProfile(String code, String redirectUri) {
        String decodedCode = decodeAuthorizationCode(code); // 입력한 코드를 디코드 하여 보내야 인식
        String accessToken = requestAccessToken(decodedCode, redirectUri);
        return requestProfile(accessToken);
    }

    private String decodeAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }

        return URLDecoder.decode(code, StandardCharsets.UTF_8);
    }

    // 구글 토큰 API 호출에 필요한 데이터를 구성
    private String requestAccessToken(String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code"); // 인가 코드 방식으로 토큰 받기
        form.put("client_id", clientId);
        form.put("redirect_uri", redirectUri);
        form.put("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.put("client_secret", clientSecret);
        }

        // 엔드포인트로 POST 요청
        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(toFormData(form)))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Google 토큰 엔드포인트 응답 비정상: status={}, body={}",
                    response.statusCode(), truncate(response.body()));
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }

        JsonNode body = readJson(response.body());
        String accessToken = body.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Google 엑세스 토큰 비어있음: response={}", truncate(response.body()));
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }

        return accessToken;
    }

    private SocialProfileDto requestProfile(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(USER_INFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Google 프로필 엔드포인트 응답 비정상: status={}, body={}",
                    response.statusCode(), truncate(response.body()));
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }

        JsonNode body = readJson(response.body());
        String providerId = body.path("id").asText(null);
        String email = body.path("email").asText(null);
        String name = body.path("name").asText(null);

        if (providerId == null || providerId.isBlank()) { // 소셜 식별 아이디가 비어있을 때
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }

        return new SocialProfileDto(providerId, email, name);
    }
    // 실제 HTTP 요청과 예외 처리는 한 곳에서 공통으로 처리한다
    private HttpResponse<String> send(HttpRequest request) {
        try {
            // 실제 HTTP 통신은 여기서 한 번에 처리
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return resp;
        }
        catch (IOException e) {
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }
    }

    private JsonNode readJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            log.error("Google 응답 JSON 파싱 실패: body={}", truncate(body), e);
            throw new BusinessException(ErrorCode.GOOGLE_LOGIN_FAILED);
        }
    }

    // 로그에 찍는 응답 바디가 너무 길어지지 않도록 최대 500자까지만 남긴다.
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

    // field=value&field=value 형태로 form body를 만든다
    private String toFormData(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    // URL 전송용으로 인코딩한다
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}