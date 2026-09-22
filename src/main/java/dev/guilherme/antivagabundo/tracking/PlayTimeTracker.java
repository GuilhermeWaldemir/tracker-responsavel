package dev.guilherme.antivagabundo.tracking;

import dev.guilherme.antivagabundo.storage.SessionRepository;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Transforma verificações periódicas ("está jogando agora?") em sessões salvas no banco.
 *
 * A sessão é estendida a cada verificação, então se o PC desligar de repente
 * perde-se no máximo o intervalo entre duas verificações.
 */
public final class PlayTimeTracker {

    private final SessionRepository repository;
    // Se passar mais que isso entre duas verificações (ex.: PC em suspensão), não conta o buraco.
    private final Duration maxGap;

    private Long currentSessionId; // null = não está jogando
    private Instant lastCheck;

    public PlayTimeTracker(SessionRepository repository, Duration maxGap) {
        this.repository = repository;
        this.maxGap = maxGap;
    }

    public void update(boolean playing, Instant now) throws SQLException {
        boolean resumedAfterGap = lastCheck != null
                && Duration.between(lastCheck, now).compareTo(maxGap) > 0;
        lastCheck = now;

        if (resumedAfterGap) {
            // A sessão anterior termina onde foi vista pela última vez; o tempo suspenso fica de fora.
            currentSessionId = null;
        }

        if (playing) {
            if (currentSessionId == null) {
                currentSessionId = repository.startSession(now);
            } else {
                repository.extendSession(currentSessionId, now);
            }
        } else if (currentSessionId != null) {
            repository.extendSession(currentSessionId, now);
            currentSessionId = null;
        }
    }
}
