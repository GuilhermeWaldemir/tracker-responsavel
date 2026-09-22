package dev.guilherme.antivagabundo.tracking;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** Utilitários de semana: a semana começa na segunda-feira às 00:00 no fuso local. */
public final class Week {

    private Week() {
    }

    public static Instant startOf(Instant moment, ZoneId zone) {
        return moment.atZone(zone)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(zone)
                .toInstant();
    }

    /** Formata como "5h 07min". */
    public static String format(Duration duration) {
        return "%dh %02dmin".formatted(duration.toHours(), duration.toMinutesPart());
    }
}
