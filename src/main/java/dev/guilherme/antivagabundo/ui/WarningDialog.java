package dev.guilherme.antivagabundo.ui;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Janela que aparece quando um jogo é aberto. */
public final class WarningDialog {

    private static final String PLAY = "Vou jogar mesmo assim";
    private static final String CLOSE = "Tem razão, fecha o jogo";

    // Só mexido na thread do Swing, então não precisa de sincronização.
    private boolean showing;

    /**
     * Mostra o aviso sem travar quem chamou. Se o usuário escolher fechar, roda {@code onCloseGame}.
     * Se já houver um aviso na tela, este é ignorado para não empilhar janelas.
     */
    public void show(String game, String playedThisWeek, Runnable onCloseGame) {
        // Toda interação com o Swing precisa acontecer na thread dele (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> {
            if (showing) {
                return;
            }
            showing = true;
            try {
                if (ask(game, playedThisWeek)) {
                    onCloseGame.run();
                }
            } finally {
                showing = false;
            }
        });
    }

    /** Devolve true se o usuário escolheu fechar o jogo. */
    private boolean ask(String game, String playedThisWeek) {
        String message = """
                <html>
                Você abriu <b>%s</b>.<br><br>
                Nesta semana você já jogou <b>%s</b>.<br><br>
                Já terminou <b>todas</b> as suas responsabilidades?<br>
                Vai jogar mesmo?
                </html>""".formatted(game, playedThisWeek);

        Object[] options = {PLAY, CLOSE};
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, CLOSE);
        JDialog dialog = pane.createDialog("Tracker Anti-Vagabundo");
        dialog.setAlwaysOnTop(true); // aparece por cima do launcher
        dialog.setVisible(true);     // bloqueia até o usuário responder
        dialog.dispose();

        return CLOSE.equals(pane.getValue());
    }
}
