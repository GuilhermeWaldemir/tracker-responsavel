package dev.guilherme.antivagabundo.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

/**
 * Guarda as sessões de jogo no SQLite. Cada sessão é um intervalo [started_at, ended_at]
 * em segundos desde 1970 (epoch).
 */
public final class SessionRepository implements AutoCloseable {

    private final Connection connection;

    public SessionRepository(Path databaseFile) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        started_at INTEGER NOT NULL,
                        ended_at   INTEGER NOT NULL
                    )""");
        }
    }

    /** Abre uma sessão nova (começa e termina no mesmo instante) e devolve o id dela. */
    public long startSession(Instant now) throws SQLException {
        String sql = "INSERT INTO sessions (started_at, ended_at) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, now.getEpochSecond());
            statement.setLong(2, now.getEpochSecond());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Estende o fim da sessão até "now". */
    public void extendSession(long sessionId, Instant now) throws SQLException {
        String sql = "UPDATE sessions SET ended_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now.getEpochSecond());
            statement.setLong(2, sessionId);
            statement.executeUpdate();
        }
    }

    /**
     * Soma o tempo jogado dentro do intervalo [from, to). Sessões que atravessam
     * as bordas (ex.: começou domingo e terminou segunda) contam só a parte de dentro.
     */
    public Duration totalPlayed(Instant from, Instant to) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(MIN(ended_at, ?) - MAX(started_at, ?)), 0)
                FROM sessions
                WHERE ended_at > ? AND started_at < ?""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, to.getEpochSecond());
            statement.setLong(2, from.getEpochSecond());
            statement.setLong(3, from.getEpochSecond());
            statement.setLong(4, to.getEpochSecond());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return Duration.ofSeconds(result.getLong(1));
            }
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
