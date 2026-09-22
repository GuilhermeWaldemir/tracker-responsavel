package dev.guilherme.antivagabundo.startup;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowsStartupTest {

    @Test
    void quotesPathsSoSpacesDoNotBreakTheCommand() {
        String command = WindowsStartup.startupCommand(
                Path.of("C:\\Program Files\\Java\\bin\\javaw.exe"),
                Path.of("C:\\Users\\Guilh\\AppData\\Roaming\\TrackerAntiVagabundo\\tracker-anti-vagabundo.jar"));

        assertEquals("\"C:\\Program Files\\Java\\bin\\javaw.exe\" -jar "
                        + "\"C:\\Users\\Guilh\\AppData\\Roaming\\TrackerAntiVagabundo\\tracker-anti-vagabundo.jar\"",
                command);
    }
}
