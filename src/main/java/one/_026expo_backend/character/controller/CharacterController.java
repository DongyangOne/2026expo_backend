package one._026expo_backend.character.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.character.dto.response.MyCharacterResponseDto;
import one._026expo_backend.character.service.CharacterService;
import one._026expo_backend.character.service.UserCharacterService;
import one._026expo_backend.global.config.auth.CurrentUser;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/character")
@RequiredArgsConstructor
@Tag(name = "character", description = "캐릭터 엔드포인트")
public class CharacterController {
    private final CharacterService characterService;
    private final UserCharacterService userCharacterService;

    @Operation(summary = "사용자 캐릭터 정보 조회", description = "로그인한 사용자의 캐릭터 정보를 조회합니다.")
    @ApiErrorExceptions({ErrorCode.USER_CHARACTER_NOT_FOUND})
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyCharacterResponseDto>> getMyCharacter(
            @CurrentUser Long userId
            ) {
        MyCharacterResponseDto response = userCharacterService.getMyCharacter(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
