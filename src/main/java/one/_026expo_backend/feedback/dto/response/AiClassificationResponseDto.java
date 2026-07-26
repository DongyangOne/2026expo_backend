package one._026expo_backend.feedback.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class AiClassificationResponseDto {

    @JsonProperty("client_id")
    private String clientId;

    private String status;
    private ClassificationDto classification;
    private ConditionsDto conditions;
    private WeightDto weight;
    private List<GuidanceDto> guidance = new ArrayList<>();
    private CodeMessageDto rejection;
    private CodeMessageDto general;
    private List<Double> bbox;

    @Getter
    @NoArgsConstructor
    public static class ClassificationDto {
        @JsonProperty("class_id")
        private Integer classId;

        @JsonProperty("class_name")
        private String className;

        private Double confidence;
    }

    @Getter
    @NoArgsConstructor
    public static class ConditionsDto {
        @JsonProperty("has_label")
        private Boolean hasLabel;

        @JsonProperty("is_dented")
        private Boolean isDented;
    }

    @Getter
    @NoArgsConstructor
    public static class WeightDto {
        @JsonProperty("value_g")
        private Double valueG;

        private Boolean anomaly;
    }

    @Getter
    @NoArgsConstructor
    public static class GuidanceDto {
        private String code;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    public static class CodeMessageDto {
        private String code;
        private String message;
    }
}
