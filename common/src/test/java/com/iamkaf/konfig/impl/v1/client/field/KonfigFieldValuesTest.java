package com.iamkaf.konfig.impl.v1.client.field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class KonfigFieldValuesTest {
    @Test
    void integerStepsUseValuesInsteadOfSliderPixels() {
        assertEquals(63, KonfigFieldValues.stepInt(64, -1, 1, 2048));
        assertEquals(65, KonfigFieldValues.stepInt(64, 1, 1, 2048));
    }

    @Test
    void integerStepsStopAtOverflowingBounds() {
        assertEquals(
                Integer.MIN_VALUE,
                KonfigFieldValues.stepInt(Integer.MIN_VALUE, -1, Integer.MIN_VALUE, Integer.MAX_VALUE)
        );
        assertEquals(
                Integer.MAX_VALUE,
                KonfigFieldValues.stepInt(Integer.MAX_VALUE, 1, Integer.MIN_VALUE, Integer.MAX_VALUE)
        );
    }

    @Test
    void longStepsUseValuesAndStopAtOverflowingBounds() {
        assertEquals(63L, KonfigFieldValues.stepLong(64L, -1, 1L, 2048L));
        assertEquals(65L, KonfigFieldValues.stepLong(64L, 1, 1L, 2048L));
        assertEquals(
                Long.MIN_VALUE,
                KonfigFieldValues.stepLong(Long.MIN_VALUE, -1, Long.MIN_VALUE, Long.MAX_VALUE)
        );
        assertEquals(
                Long.MAX_VALUE,
                KonfigFieldValues.stepLong(Long.MAX_VALUE, 1, Long.MIN_VALUE, Long.MAX_VALUE)
        );
    }
}
