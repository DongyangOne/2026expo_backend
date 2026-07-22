package one._026expo_backend.feedback.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.DetectionStatus;

import java.util.ArrayList;
import java.util.List;

@Builder
@Schema(description = "AI 하드웨어 쓰레기 분류 결과 수신 요청 DTO")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackRequestDto {

    @Schema(description = "검사 시작 API에서 발급한 검사 고유 식별자", example = "d8e4a4f7-3eec-488b-bff5-ed9f67edb8f0")
    @JsonProperty("client_id")
    @NotBlank(message = "clientId는 필수입니다.")
    private String clientId;

    @Schema(description = "AI 쓰레기 처리 판정 결과", example = "REJECTED",
            allowableValues = {"ALLOWED", "REJECTED", "GENERAL_WASTE", "NOT_DETECTED"})
    @NotNull(message = "AI 처리 결과는 필수입니다.")
    private DetectionStatus status;

    @Schema(description = "AI 쓰레기 분류 결과")
    private ClassificationDto classification;

    @Schema(description = "라벨 및 압착 상태 판정 결과")
    private ConditionsDto conditions;

    @Schema(description = "무게 센서 측정 및 이상 여부")
    private WeightDto weight;

    @Builder.Default
    @ArraySchema(arraySchema = @Schema(description = "재처리 후 다시 투입할 수 있는 쓰레기의 안내 목록"),
            schema = @Schema(implementation = GuidanceDto.class))
    private List<GuidanceDto> guidance = new ArrayList<>();

    @Schema(description = "유리병, 건전지 등 수거 비허용 쓰레기 정보")
    private CodeMessageDto rejection;

    @Schema(description = "일반쓰레기로 처리해야 하는 경우의 안내 정보")
    private CodeMessageDto general;

    @Schema(description = "AI가 감지한 객체 영역 [x1, y1, x2, y2]", example = "[104.0, 149.3, 235.2, 280.3]")
    private List<Double> bbox;

    @Getter
    @NoArgsConstructor
    @Schema(description = "AI 쓰레기 분류 결과")
    public static class ClassificationDto {

    @Schema(description = "AI 모델의 쓰레기 클래스 ID", example = "4")
    @JsonProperty("class_id")
    private Integer classId;

    @Schema(description = "AI가 판별한 쓰레기 종류", example = "styrofoam",
            allowableValues = {"plastic", "pet", "can", "paper", "vinyl", "glass", "battery", "fluorescent", "styrofoam"})
    @JsonProperty("class_name")
    private String className;

    @Schema(description = "AI 쓰레기 분류 신뢰도(0~1)", example = "0.9258")
    private Double confidence;
        }

    @Getter
    @NoArgsConstructor
    @Schema(description = "쓰레기 상태 판정 결과")
    public static class ConditionsDto {

    @Schema(description = "라벨 부착 여부. 검사 대상이 아니면 null", example = "false", nullable = true)
    @JsonProperty("has_label")
    private Boolean hasLabel;

    @Schema(description = "압착 또는 찌그러짐 여부. 검사 대상이 아니면 null", example = "true", nullable = true)
    @JsonProperty("is_dented")
    private Boolean isDented;
        }

    @Getter
    @NoArgsConstructor
    @Schema(description = "무게 센서 판정 결과")
    public static class WeightDto {

    @Schema(description = "무게 센서 측정값(g). 미측정 시 null", example = "28.0", nullable = true)
    @JsonProperty("value_g")
    private Double valueG;

    @Schema(description = "정상 무게 범위 초과 여부", example = "false")
    private Boolean anomaly;
        }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재처리 안내 정보")
    public static class GuidanceDto {

        @Schema(description = "재처리 안내 코드", example = "REMOVE_LABEL",
                    allowableValues = {"EMPTY_CONTENTS", "REMOVE_LABEL", "COMPRESS", "REMOVE_FOREIGN_MATERIAL"})
            private String code;

        @Schema(description = "사용자에게 표시할 재처리 안내 문구", example = "라벨을 제거한 후 다시 넣어 주세요.")
            private String message;
        }

        @Getter
        @NoArgsConstructor
        @Schema(description = "처리 코드 및 사용자 안내 문구")
        public static class CodeMessageDto {

            @Schema(description = "거부 또는 일반쓰레기 사유 코드", example = "STYROFOAM")
            private String code;

            @Schema(description = "사용자에게 표시할 안내 문구", example = "스티로폼은 이 기기에서 처리하기 어려워요. 스티로폼 전용 분리수거함을 이용해 주세요.")
            private String message;
        }
    }