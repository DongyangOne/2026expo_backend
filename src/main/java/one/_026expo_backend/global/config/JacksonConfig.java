package one._026expo_backend.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // KakaoOAuthClient가 주입받을 ObjectMapper 빈을 Spring에 명시적으로 등록
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
