package dev.guilherme.antivagabundo.games;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Conjunto de executáveis que contam como jogo (ex.: "steam.exe"). */
public final class GameList {

    private static final String DEFAULT_RESOURCE = "/jogos.txt";

    private final Set<String> processNames;

    private GameList(Set<String> processNames) {
        this.processNames = Set.copyOf(processNames);
    }

    /** Monta a lista a partir das linhas do arquivo, ignorando linhas vazias e comentários (#). */
    public static GameList parse(List<String> lines) {
        Set<String> names = lines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return new GameList(names);
    }

    /**
     * Lê a lista do arquivo do usuário. Se ele ainda não existir, cria uma cópia
     * da lista padrão que vem dentro do .jar, para o usuário poder editar depois.
     */
    public static GameList loadOrCreate(Path file) throws IOException {
        if (Files.notExists(file)) {
            try (InputStream defaults = GameList.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                if (defaults == null) {
                    throw new IOException("Lista padrão de jogos não encontrada: " + DEFAULT_RESOURCE);
                }
                Files.createDirectories(file.getParent());
                Files.copy(defaults, file);
            }
        }
        return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    public boolean isGame(String processName) {
        return processNames.contains(processName.toLowerCase(Locale.ROOT));
    }

    public Set<String> processNames() {
        return processNames;
    }
}
