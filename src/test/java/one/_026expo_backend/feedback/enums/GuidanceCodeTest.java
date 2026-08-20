package one._026expo_backend.feedback.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuidanceCodeTest {

    @Test
    void ai서버와_합의한_코드만_제공한다() {
        List<String> codes = Arrays.stream(GuidanceCode.values())
                .map(Enum::name)
                .toList();

        assertEquals(List.of(
                "EMPTY_CONTENTS",
                "WEIGHT_ANOMALY",
                "FOREIGN_MATERIAL",
                "REMOVE_LABEL",
                "COMPRESS"
        ), codes);
    }
}
