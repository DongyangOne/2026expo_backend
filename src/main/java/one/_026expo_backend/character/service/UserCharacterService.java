package one._026expo_backend.character.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.character.repository.UserCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCharacterService {
    private final UserCharacterRepository userCharacterRepository;
}
