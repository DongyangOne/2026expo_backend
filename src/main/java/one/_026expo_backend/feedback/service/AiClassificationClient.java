package one._026expo_backend.feedback.service;

import one._026expo_backend.feedback.dto.request.TabletClassificationRequestDto;
import one._026expo_backend.feedback.dto.response.AiClassificationResponseDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class AiClassificationClient {

    @Value("${ai.server.base-url:https://ai.oneexpo.kro.kr}")
    private String baseUrl;

    @Value("${ai.server.api-key:}")
    private String apiKey;

    public AiClassificationResponseDto detect(TabletClassificationRequestDto request) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED);
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("client_id", request.getClientId());
        body.add("image", request.getImage().getResource());

        if (request.getWeightG() != null) {
            body.add("weight_g", request.getWeightG());
        }

        try {
            return RestClient
                    .builder()
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri("/api/v1/detect")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AiClassificationResponseDto.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED);
        }
    }
}
