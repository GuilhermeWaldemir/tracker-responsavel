package dev.guilherme.trackerresponsavel.ui;

import dev.guilherme.trackerresponsavel.startup.WindowsStartup;

import javax.swing.JOptionPane;
import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.image.BufferedImage;

/** Ícone na bandeja do Windows (perto do relógio) com as horas da semana e a opção de sair. */
public final class TrayIcon {

    private final java.awt.TrayIcon icon;
    private final MenuItem hoursItem = new MenuItem();

    public TrayIcon(WindowsStartup startup, Runnable onExit) throws AWTException {
        hoursItem.setEnabled(false); // só informativo

        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(event -> onExit.run());

        PopupMenu menu = new PopupMenu();
        menu.add(hoursItem);
        menu.addSeparator();
        menu.add(startupItem(startup));
        menu.add(exitItem);

        icon = new java.awt.TrayIcon(drawIcon(), "Tracker Responsável", menu);
        icon.setImageAutoSize(true);
        SystemTray.getSystemTray().add(icon);
    }

    public void showPlayedThisWeek(String played) {
        EventQueue.invokeLater(() -> {
            hoursItem.setLabel("Esta semana: " + played);
            icon.setToolTip("Tracker Responsável\nEsta semana: " + played);
        });
    }

    public void remove() {
        SystemTray.getSystemTray().remove(icon);
    }

    private static CheckboxMenuItem startupItem(WindowsStartup startup) {
        CheckboxMenuItem item = new CheckboxMenuItem("Iniciar com o Windows");
        if (!startup.isAvailable()) {
            item.setLabel("Iniciar com o Windows (só rodando pelo .jar)");
            item.setEnabled(false);
            return item;
        }

        try {
            item.setState(startup.isEnabled());
        } catch (Exception e) {
            e.printStackTrace();
        }

        item.addItemListener(event -> {
            boolean enable = item.getState();
            try {
                if (enable) {
                    startup.enable();
                } else {
                    startup.disable();
                }
            } catch (Exception e) {
                item.setState(!enable); // desfaz a marcação, já que não funcionou
                JOptionPane.showMessageDialog(null, e.getMessage(),
                        "Tracker Responsável", JOptionPane.ERROR_MESSAGE);
            }
        });
        return item;
    }

    // Desenha o ícone no código para não depender de arquivo de imagem.
    private static BufferedImage drawIcon() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xD9, 0x3B, 0x3B));
        g.fillOval(0, 0, 32, 32);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString("!", 13, 24);
        g.dispose();
        return image;
    }
}
