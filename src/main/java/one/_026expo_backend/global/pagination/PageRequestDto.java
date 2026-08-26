package one._026expo_backend.global.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {

    @Schema(example = "0", description = "페이지 번호 (기본값: 0)")
    @NotNull(message = "페이지 번호를 입력해주세요.")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private Integer page = 0;

    @Schema(example ="10", description = "페이지 크기 (기본값: 10)")
    @NotNull(message = "페이지 크기를 입력해주세요.")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하이어야 합니다.")
    private Integer pageSize = 10;

    @Schema(hidden = true)
    public int getOffset() {
        return page * pageSize;
    }
}