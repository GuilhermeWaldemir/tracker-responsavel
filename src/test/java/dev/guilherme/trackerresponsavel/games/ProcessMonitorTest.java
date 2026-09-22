package dev.guilherme.trackerresponsavel.games;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessMonitorTest {

    private final GameList games = GameList.parse(List.of("steam.exe", "cs2.exe"));
    // Lista de processos "falsa" que o teste controla.
    private final Set<String> running = new HashSet<>();
    private final ProcessMonitor monitor = new ProcessMonitor(games, () -> running);

    @Test
    void ignoresProcessesThatAreNotGames() {
        running.addAll(Set.of("chrome.exe", "explorer.exe"));

        ProcessMonitor.Snapshot snapshot = monitor.check();

        assertFalse(snapshot.isPlaying());
        assertTrue(snapshot.newlyOpened().isEmpty());
    }

    @Test
    void reportsGameOnlyOnTheCheckItOpened() {
        running.add("steam.exe");
        assertEquals(Set.of("steam.exe"), monitor.check().newlyOpened());

        ProcessMonitor.Snapshot secondCheck = monitor.check();
        assertTrue(secondCheck.newlyOpened().isEmpty());
        assertTrue(secondCheck.isPlaying());
    }

    @Test
    void reportsGameAgainAfterItIsClosedAndReopened() {
        running.add("cs2.exe");
        monitor.check();

        running.remove("cs2.exe");
        assertFalse(monitor.check().isPlaying());

        running.add("cs2.exe");
        assertEquals(Set.of("cs2.exe"), monitor.check().newlyOpened());
    }

    @Test
    void reportsOnlyTheNewGameWhenAnotherIsAlreadyOpen() {
        running.add("steam.exe");
        monitor.check();

        running.add("cs2.exe");

        assertEquals(Set.of("cs2.exe"), monitor.check().newlyOpened());
    }
}
