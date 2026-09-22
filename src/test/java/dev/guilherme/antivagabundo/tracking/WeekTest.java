package dev.guilherme.antivagabundo.tracking;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeekTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void weekStartsOnMondayAtMidnightLocalTime() {
        // Quarta-feira, 23/09/2026, 15:30 em São Paulo
        Instant wednesday = Instant.parse("2026-09-23T18:30:00Z");

        // Segunda-feira, 21/09/2026, 00:00 em São Paulo = 03:00 UTC
        assertEquals(Instant.parse("2026-09-21T03:00:00Z"), Week.startOf(wednesday, SAO_PAULO));
    }

    @Test
    void mondayItselfBelongsToTheCurrentWeek() {
        Instant mondayMorning = Instant.parse("2026-09-21T12:00:00Z");

        assertEquals(Instant.parse("2026-09-21T03:00:00Z"), Week.startOf(mondayMorning, SAO_PAULO));
    }

    @Test
    void formatsHoursAndMinutes() {
        assertEquals("5h 07min", Week.format(Duration.ofMinutes(307)));
        assertEquals("0h 00min", Week.format(Duration.ZERO));
        assertEquals("26h 30min", Week.format(Duration.ofMinutes(1590)));
    }
}
