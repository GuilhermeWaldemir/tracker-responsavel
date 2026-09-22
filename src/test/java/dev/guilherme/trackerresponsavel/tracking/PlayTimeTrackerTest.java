package dev.guilherme.trackerresponsavel.tracking;

import dev.guilherme.trackerresponsavel.storage.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayTimeTrackerTest {

    private static final Instant T0 = Instant.parse("2026-09-21T20:00:00Z");

    @TempDir
    Path dir;
    private SessionRepository repository;
    private PlayTimeTracker tracker;

    @BeforeEach
    void setUp() throws Exception {
        repository = new SessionRepository(dir.resolve("test.db"));
        tracker = new PlayTimeTracker(repository, Duration.ofMinutes(1));
    }

    @AfterEach
    void tearDown() throws Exception {
        repository.close();
    }

    @Test
    void countsTimeWhilePlaying() throws Exception {
        playFor(T0, Duration.ofMinutes(10));
        tracker.update(false, T0.plus(Duration.ofMinutes(10)).plusSeconds(5));

        assertEquals(Duration.ofMinutes(10).plusSeconds(5), totalAround(T0));
    }

    @Test
    void doesNotCountTimeWhileNotPlaying() throws Exception {
        tracker.update(false, T0);
        tracker.update(false, T0.plusSeconds(30));

        assertEquals(Duration.ZERO, totalAround(T0));
    }

    @Test
    void doesNotCountTimeTheComputerWasAsleep() throws Exception {
        playFor(T0, Duration.ofMinutes(5));

        // O PC "dormiu" por 8 horas com o jogo aberto e acordou ainda jogando.
        Instant wakeUp = T0.plus(Duration.ofHours(8));
        playFor(wakeUp, Duration.ofMinutes(5));

        assertEquals(Duration.ofMinutes(10), totalAround(T0));
    }

    /** Simula verificações a cada 5 segundos, jogando, de {@code start} até {@code start + length}. */
    private void playFor(Instant start, Duration length) throws Exception {
        for (long s = 0; s <= length.toSeconds(); s += 5) {
            tracker.update(true, start.plusSeconds(s));
        }
    }

    private Duration totalAround(Instant moment) throws Exception {
        return repository.totalPlayed(moment.minus(Duration.ofDays(1)), moment.plus(Duration.ofDays(1)));
    }
}
