package dev.guilherme.antivagabundo.startup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Liga/desliga a inicialização junto com o Windows.
 *
 * Funciona registrando um comando na chave "Run" do Registro do usuário, que o Windows
 * executa a cada login. O .jar é copiado para a pasta de dados do app antes, para que
 * recompilar o projeto (ou apagar a pasta target) não quebre a inicialização.
 */
public final class WindowsStartup {

    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "TrackerAntiVagabundo";

    private final Path installedJar;

    public WindowsStartup(Path dataDir) {
        this.installedJar = dataDir.resolve("tracker-anti-vagabundo.jar");
    }

    /** Só dá para ativar rodando a partir do .jar (pela IDE o app roda de uma pasta de classes). */
    public boolean isAvailable() {
        return runningJar().isPresent();
    }

    public boolean isEnabled() throws IOException, InterruptedException {
        return reg("query", RUN_KEY, "/v", VALUE_NAME) == 0;
    }

    public void enable() throws IOException, InterruptedException {
        Path jar = runningJar().orElseThrow(() -> new IOException("O app não está rodando a partir de um .jar."));
        if (Files.notExists(installedJar) || !Files.isSameFile(jar, installedJar)) {
            Files.copy(jar, installedJar, StandardCopyOption.REPLACE_EXISTING);
        }

        String command = startupCommand(javaw(), installedJar);
        // No reg.exe, aspas dentro do valor precisam ser escapadas com \"
        int exitCode = reg("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ",
                "/d", command.replace("\"", "\\\""), "/f");
        if (exitCode != 0) {
            throw new IOException("Não foi possível registrar a inicialização (reg.exe saiu com " + exitCode + ").");
        }
    }

    public void disable() throws IOException, InterruptedException {
        if (isEnabled()) {
            int exitCode = reg("delete", RUN_KEY, "/v", VALUE_NAME, "/f");
            if (exitCode != 0) {
                throw new IOException("Não foi possível remover a inicialização (reg.exe saiu com " + exitCode + ").");
            }
        }
    }

    /** Ex.: "C:\Program Files\Java\bin\javaw.exe" -jar "C:\Users\...\tracker-anti-vagabundo.jar" */
    static String startupCommand(Path javaw, Path jar) {
        return "\"" + javaw + "\" -jar \"" + jar + "\"";
    }

    // javaw.exe é o Java sem janela de terminal.
    private static Path javaw() {
        return Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
    }

    private static Optional<Path> runningJar() {
        try {
            Path location = Path.of(WindowsStartup.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return location.toString().endsWith(".jar") ? Optional.of(location) : Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private static int reg(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("reg"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        return process.waitFor();
    }
}
