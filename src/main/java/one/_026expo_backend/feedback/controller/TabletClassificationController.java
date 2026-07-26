package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.request.TabletClassificationRequestDto;
import one._026expo_backend.feedback.dto.response.TabletClassificationResponseDto;
import one._026expo_backend.feedback.service.TabletClassificationService;
import one._026expo_backend.global.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tablet/classifications")
@RequiredArgsConstructor
@Validated
@Tag(name = "tabletClassification", description = "태블릿 AI 재활용품 분류 결과 API")
public class TabletClassificationController {

    private final TabletClassificationService tabletClassificationService;

    @Operation(summary = "태블릿 AI 분류 결과 조회", description = "clientId 기준으로 저장된 AI 분류 결과를 조회합니다. 결과가 없거나 아직 완료되지 않았다면 WAITING을 반환합니다.")
    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<TabletClassificationResponseDto>> getResult(
            @PathVariable String clientId
    ) {
        TabletClassificationResponseDto response = tabletClassificationService.getResult(clientId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "태블릿 AI 분류 실행 및 결과 저장", description = "태블릿에서 전달한 이미지를 AI 서버에 보내고, AI 응답을 백엔드 응답 DTO로 변환해 DB에 저장합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TabletClassificationResponseDto>> classify(
            @Valid @ModelAttribute TabletClassificationRequestDto request
    ) {
        TabletClassificationResponseDto response = tabletClassificationService.classify(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
