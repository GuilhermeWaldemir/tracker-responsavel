package dev.guilherme.antivagabundo.games;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Compara os processos abertos agora com os da última verificação
 * para descobrir quais jogos acabaram de ser abertos.
 */
public final class ProcessMonitor {

    /** Resultado de uma verificação. */
    public record Snapshot(Set<String> runningGames, Set<String> newlyOpened) {

        public boolean isPlaying() {
            return !runningGames.isEmpty();
        }
    }

    private final GameList games;
    // Quem fornece os nomes dos processos. Em produção é o Windows; nos testes, uma lista falsa.
    private final Supplier<Set<String>> runningProcesses;
    private Set<String> previousGames = Set.of();

    public ProcessMonitor(GameList games, Supplier<Set<String>> runningProcesses) {
        this.games = games;
        this.runningProcesses = runningProcesses;
    }

    public Snapshot check() {
        Set<String> runningGames = runningProcesses.get().stream()
                .filter(games::isGame)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> newlyOpened = new HashSet<>(runningGames);
        newlyOpened.removeAll(previousGames);

        previousGames = runningGames;
        return new Snapshot(Set.copyOf(runningGames), Set.copyOf(newlyOpened));
    }

    /** Nomes dos executáveis de todos os processos abertos no sistema (ex.: "steam.exe"). */
    public static Set<String> systemProcessNames() {
        return ProcessHandle.allProcesses()
                .map(ProcessMonitor::executableName)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    /** Fecha todos os processos com esse nome de executável. */
    public static void kill(String processName) {
        ProcessHandle.allProcesses()
                .filter(process -> executableName(process)
                        .map(name -> name.equalsIgnoreCase(processName))
                        .orElse(false))
                .forEach(ProcessHandle::destroy);
    }

    // O Windows às vezes não informa o caminho (ex.: processos do sistema), por isso o Optional.
    private static Optional<String> executableName(ProcessHandle process) {
        return process.info().command()
                .map(command -> Path.of(command).getFileName().toString().toLowerCase(Locale.ROOT));
    }
}
