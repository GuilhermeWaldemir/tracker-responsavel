package dev.guilherme.trackerresponsavel.startup;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Garante que só uma cópia do app rode por vez, trancando um arquivo.
 *
 * Quem cuida da trava é o sistema operacional: se o app fechar ou travar,
 * ela é liberada sozinha, então o app nunca fica "preso" sem conseguir abrir.
 */
public final class SingleInstanceLock implements AutoCloseable {

    // Precisa continuar referenciado enquanto o app roda: se o canal for fechado, a trava é solta.
    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** Tenta pegar a trava. Devolve vazio se outra cópia do app já estiver com ela. */
    public static Optional<SingleInstanceLock> tryAcquire(Path lockFile) throws IOException {
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) { // outro processo já trancou
                channel.close();
                return Optional.empty();
            }
            return Optional.of(new SingleInstanceLock(channel, lock));
        } catch (OverlappingFileLockException e) { // este mesmo processo já trancou
            channel.close();
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        lock.release();
        channel.close();
    }
}
