package dev.guilherme.antivagabundo.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionRepositoryTest {

    private static final Instant MONDAY = Instant.parse("2026-09-21T00:00:00Z");
    private static final Instant NEXT_MONDAY = MONDAY.plus(Duration.ofDays(7));

    @TempDir
    Path dir;
    private SessionRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new SessionRepository(dir.resolve("test.db"));
    }

    @AfterEach
    void tearDown() throws Exception {
        repository.close();
    }

    @Test
    void sumsSessionsInsideTheInterval() throws Exception {
        session(MONDAY.plus(Duration.ofHours(10)), Duration.ofHours(2));
        session(MONDAY.plus(Duration.ofDays(2)), Duration.ofMinutes(30));

        assertEquals(Duration.ofMinutes(150), repository.totalPlayed(MONDAY, NEXT_MONDAY));
    }

    @Test
    void countsOnlyThePartOfASessionInsideTheInterval() throws Exception {
        // Domingo 23:00 até segunda 01:00: só a 1 hora de segunda é desta semana.
        session(MONDAY.minus(Duration.ofHours(1)), Duration.ofHours(2));

        assertEquals(Duration.ofHours(1), repository.totalPlayed(MONDAY, NEXT_MONDAY));
    }

    @Test
    void ignoresSessionsOutsideTheInterval() throws Exception {
        session(MONDAY.minus(Duration.ofDays(3)), Duration.ofHours(4));

        assertEquals(Duration.ZERO, repository.totalPlayed(MONDAY, NEXT_MONDAY));
    }

    private void session(Instant start, Duration length) throws Exception {
        long id = repository.startSession(start);
        repository.extendSession(id, start.plus(length));
    }
}
