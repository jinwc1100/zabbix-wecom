package com.sla.notifier.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExcludeTimesTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 1, 11, 0, 0);

    @Test
    void yesLongerThanOneSecondUsesStartPlusOne() {
        assertEquals(START.plusSeconds(1), ExcludeTimes.derive("是", START, END));
    }

    @Test
    void yesUsesStartPlusOneEvenWhenNotBeforeEnd() {
        assertEquals(START.plusSeconds(1), ExcludeTimes.derive("是", START, START));
        LocalDateTime oneSec = START.plusSeconds(1);
        assertEquals(oneSec, ExcludeTimes.derive("是", START, oneSec));
    }

    @Test
    void noUsesDownEnd() {
        assertEquals(END, ExcludeTimes.derive("否", START, END));
        LocalDateTime sentinel = LocalDateTime.of(9999, 12, 31, 0, 0, 0);
        assertEquals(sentinel, ExcludeTimes.derive(" 否 ", START, sentinel));
    }

    @Test
    void blankIsNull() {
        assertNull(ExcludeTimes.derive(null, START, END));
        assertNull(ExcludeTimes.derive("  ", START, END));
    }
}
