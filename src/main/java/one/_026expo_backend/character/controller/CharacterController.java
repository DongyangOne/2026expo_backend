package one._026expo_backend.character.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.character.service.CharacterService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/character")
@RequiredArgsConstructor
@Tag(name = "character", description = "캐릭터 엔드포인트")
public class CharacterController {
    private final CharacterService characterService;

}