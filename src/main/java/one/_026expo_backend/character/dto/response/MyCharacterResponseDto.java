package one._026expo_backend.character.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "사용자 캐릭터 정보 응답 DTO")
public class MyCharacterResponseDto {
    @Schema(description = "캐릭터 고유 ID", example = "1")
    private Long characterId;

    @Schema(description = "캐릭터 이름", example = "알")
    private String characterName;

    @Schema(description = "MinIO 이미지 URL", example = "https://minio-storage.../.../0.png")
    private String imageUrl;

    @Schema(description = "현재 진화 단계", example = "1")
    private Integer evolutionStage;

    public static MyCharacterResponseDto of(
            Long characterId, String characterName, String imageUrl, Integer evolutionStage) {
        return MyCharacterResponseDto.builder()
                .characterId(characterId)
                .characterName(characterName)
                .imageUrl(imageUrl)
                .evolutionStage(evolutionStage)
                .build();
    }
}
