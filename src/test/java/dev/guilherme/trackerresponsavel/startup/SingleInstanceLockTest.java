package dev.guilherme.trackerresponsavel.startup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleInstanceLockTest {

    @TempDir
    Path dir;

    @Test
    void secondInstanceCannotAcquireTheLock() throws Exception {
        Path lockFile = dir.resolve("tracker.lock");

        try (SingleInstanceLock first = SingleInstanceLock.tryAcquire(lockFile).orElseThrow()) {
            Optional<SingleInstanceLock> second = SingleInstanceLock.tryAcquire(lockFile);

            assertTrue(second.isEmpty());
        }
    }

    @Test
    void lockCanBeAcquiredAgainAfterRelease() throws Exception {
        Path lockFile = dir.resolve("tracker.lock");

        SingleInstanceLock.tryAcquire(lockFile).orElseThrow().close();

        Optional<SingleInstanceLock> again = SingleInstanceLock.tryAcquire(lockFile);

        assertTrue(again.isPresent());
        again.get().close();
    }
}
