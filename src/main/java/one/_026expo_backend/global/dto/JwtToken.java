package one._026expo_backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class JwtToken {
    private String grantType;
    private String accessToken;
}
