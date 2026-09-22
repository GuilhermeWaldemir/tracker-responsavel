package dev.guilherme.trackerresponsavel;

import dev.guilherme.trackerresponsavel.games.GameList;
import dev.guilherme.trackerresponsavel.games.ProcessMonitor;
import dev.guilherme.trackerresponsavel.startup.SingleInstanceLock;
import dev.guilherme.trackerresponsavel.startup.WindowsStartup;
import dev.guilherme.trackerresponsavel.storage.SessionRepository;
import dev.guilherme.trackerresponsavel.tracking.PlayTimeTracker;
import dev.guilherme.trackerresponsavel.tracking.Week;
import dev.guilherme.trackerresponsavel.ui.TrayIcon;
import dev.guilherme.trackerresponsavel.ui.WarningDialog;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.SystemTray;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Main {

    private static final Duration CHECK_INTERVAL = Duration.ofSeconds(5);
    private static final Duration MAX_GAP = Duration.ofMinutes(1);

    private final Clock clock = Clock.systemDefaultZone();
    private final ProcessMonitor monitor;
    private final SessionRepository repository;
    private final PlayTimeTracker tracker;
    private final WarningDialog warning = new WarningDialog();
    private final TrayIcon tray;
    private final SingleInstanceLock instanceLock;
    // Uma única thread faz as verificações e é a única que usa o banco.
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Main(Path dataDir, SingleInstanceLock instanceLock) throws Exception {
        this.instanceLock = instanceLock;
        GameList games = GameList.loadOrCreate(dataDir.resolve("jogos.txt"));
        monitor = new ProcessMonitor(games, ProcessMonitor::systemProcessNames);
        repository = new SessionRepository(dataDir.resolve("tracker.db"));
        tracker = new PlayTimeTracker(repository, MAX_GAP);
        tray = new TrayIcon(new WindowsStartup(dataDir), this::exit);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        if (!SystemTray.isSupported()) {
            System.err.println("A bandeja do sistema não é suportada neste computador.");
            System.exit(1);
        }

        Path dataDir = Path.of(System.getenv().getOrDefault("APPDATA", System.getProperty("user.home")),
                "TrackerResponsavel");
        Files.createDirectories(dataDir);

        Optional<SingleInstanceLock> lock = SingleInstanceLock.tryAcquire(dataDir.resolve("tracker.lock"));
        if (lock.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "O Tracker Responsável já está rodando.\nProcure o ícone perto do relógio.",
                    "Tracker Responsável", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }

        new Main(dataDir, lock.get()).start();
    }

    private void start() {
        scheduler.scheduleWithFixedDelay(this::checkSafely, 0, CHECK_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    // Se uma exceção escapar de uma tarefa agendada, o executor para de rodá-la em silêncio.
    // Por isso capturamos tudo aqui: um erro numa verificação não pode matar o app.
    private void checkSafely() {
        try {
            check();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void check() throws SQLException {
        Instant now = clock.instant();
        ProcessMonitor.Snapshot snapshot = monitor.check();
        tracker.update(snapshot.isPlaying(), now);

        String playedThisWeek = Week.format(playedThisWeek(now));
        tray.showPlayedThisWeek(playedThisWeek);

        for (String game : snapshot.newlyOpened()) {
            warning.show(game, playedThisWeek, () -> ProcessMonitor.kill(game));
        }
    }

    private Duration playedThisWeek(Instant now) throws SQLException {
        return repository.totalPlayed(Week.startOf(now, clock.getZone()), now);
    }

    private void exit() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
            repository.close();
            instanceLock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        tray.remove();
        System.exit(0);
    }
}
