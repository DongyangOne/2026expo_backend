package one._026expo_backend.global.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {

    @Schema(example = "0", description = "페이지 번호")
    @Min(0)
    private Integer page = 0;

    @Schema(example ="10", description = "페이지 크기")
    @Min(1)
    @Max(100)
    private Integer pageSize = 10;

    @Schema(hidden = true)
    public int getOffset() {
        return page * pageSize;
    }
}