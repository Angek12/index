package com.index.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.index.platform.domain.Frequency;
import com.index.platform.service.StandingOrderService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StandingOrderDatesTest {

    @Test
    void weeklyAddsSevenDays() {
        assertEquals(LocalDate.of(2026, 11, 16), StandingOrderService.nextDate(LocalDate.of(2026, 11, 9), Frequency.WEEKLY));
    }

    @Test
    void everyTwoWeeksAddsFourteenDays() {
        assertEquals(LocalDate.of(2026, 11, 23), StandingOrderService.nextDate(LocalDate.of(2026, 11, 9), Frequency.EVERY_2_WEEKS));
    }

    @Test
    void monthlyKeepsTheDayOrUsesTheLastDay() {
        assertEquals(LocalDate.of(2026, 12, 9), StandingOrderService.nextDate(LocalDate.of(2026, 11, 9), Frequency.MONTHLY));
        assertEquals(LocalDate.of(2027, 2, 28), StandingOrderService.nextDate(LocalDate.of(2027, 1, 31), Frequency.MONTHLY));
    }
}
