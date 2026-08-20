package one._026expo_backend.feedback.service;

import one._026expo_backend.feedback.enums.WasteType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FeedbackDetailServiceTest {

    @Test
    void 합의된_무게와_이물질_코드에_맞는_영상을_선택한다() {
        assertEquals(
                "feedback_can_waterOff.mp4",
                FeedbackDetailService.resolveVideoFileName(WasteType.CAN, "EMPTY_CONTENTS")
        );
        assertEquals(
                "feedback_plastic_waterOff.mp4",
                FeedbackDetailService.resolveVideoFileName(WasteType.PLASTIC, "EMPTY_CONTENTS")
        );
        assertEquals(
                "feedback_paper_weight.mp4",
                FeedbackDetailService.resolveVideoFileName(WasteType.PAPER, "WEIGHT_ANOMALY")
        );
        assertEquals(
                "feedback_vinly_weight.mp4",
                FeedbackDetailService.resolveVideoFileName(WasteType.VINYL, "WEIGHT_ANOMALY")
        );
        assertEquals(
                "feedback_plastic_foreign.mp4",
                FeedbackDetailService.resolveVideoFileName(WasteType.PLASTIC, "FOREIGN_MATERIAL")
        );
    }

    @Test
    void 폐기한_이전_코드는_하드코딩_영상에_매핑하지_않는다() {
        assertNull(FeedbackDetailService.resolveVideoFileName(WasteType.PAPER, "EMPTY_CONTENTS"));
        assertNull(FeedbackDetailService.resolveVideoFileName(WasteType.VINYL, "EMPTY_CONTENTS"));
        assertNull(FeedbackDetailService.resolveVideoFileName(WasteType.PLASTIC, "REMOVE_FOREIGN_MATERIAL"));
    }
}
