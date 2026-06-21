package one._026expo_backend.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.user.enums.WithdrawReasonType;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
@Schema(description = "회원탈퇴 요청 DTO")
public class UserWithdrawRequestDto {

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Schema(description = "회원탈퇴 전 비밀번호 재확인 값", example = "userPassword123!")
    private String password;

    @NotNull(message = "탈퇴 사유를 선택해 주세요.")
    @Schema(
            description = "회원탈퇴 사유",
            example = "LOW_FREQUENCY",
            allowableValues = {"DELETE_RECORD", "SERVICE_ERROR", "LOW_FREQUENCY", "ETC"}
    )
    private WithdrawReasonType withdrawReason;

    @Size(max = 500, message = "탈퇴 사유는 500자 이하로 입력해 주세요.")
    @Schema(
            description = "기타 사유 상세 입력값. ETC 선택 시 필수입니다.",
            example = "서비스를 자주 사용하지 않아서 탈퇴합니다.",
            nullable = true
    )
    private String withdrawReasonDetail;

    /**
     * 기타 사유 선택 시에만 상세 입력을 강제해,
     * 화면 분기와 서버 검증 규칙이 서로 어긋나지 않도록 맞춘다.
     */
    @JsonIgnore
    @AssertTrue(message = "기타 사유를 선택한 경우 상세 사유를 입력해 주세요.")
    public boolean isWithdrawReasonDetailValid() {
        if (withdrawReason != WithdrawReasonType.ETC) {
            return true;
        }
        return StringUtils.hasText(withdrawReasonDetail);
    }
}
