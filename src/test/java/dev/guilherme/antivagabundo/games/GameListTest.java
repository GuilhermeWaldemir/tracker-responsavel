package dev.guilherme.antivagabundo.games;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameListTest {

    @Test
    void ignoresCommentsAndBlankLines() {
        GameList games = GameList.parse(List.of("# comentário", "", "  steam.exe  ", "cs2.exe"));

        assertEquals(Set.of("steam.exe", "cs2.exe"), games.processNames());
    }

    @Test
    void matchesIgnoringCase() {
        GameList games = GameList.parse(List.of("EpicGamesLauncher.exe"));

        assertTrue(games.isGame("epicgameslauncher.EXE"));
        assertFalse(games.isGame("chrome.exe"));
    }

    @Test
    void createsDefaultFileWhenMissing(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config").resolve("jogos.txt");

        GameList games = GameList.loadOrCreate(file);

        assertTrue(Files.exists(file));
        assertTrue(games.isGame("steam.exe"));
    }
}
