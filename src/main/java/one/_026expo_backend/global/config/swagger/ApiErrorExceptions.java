package one._026expo_backend.global.config.swagger;

import one._026expo_backend.global.enums.ErrorCode;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorExceptions {
    ErrorCode[] value();
}