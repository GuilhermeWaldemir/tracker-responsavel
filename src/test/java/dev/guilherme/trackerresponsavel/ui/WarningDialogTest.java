package dev.guilherme.trackerresponsavel.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningDialogTest {

    @Test
    void messageShowsGameAndHoursInBold() {
        String message = WarningDialog.message("steam.exe", "7h 45min");

        assertTrue(message.contains("<b>steam.exe</b>"));
        assertTrue(message.contains("<b>7h 45min</b>"));
    }

    /**
     * O JOptionPane cria um rótulo por linha da mensagem. Se a mensagem tivesse quebras de linha,
     * só o primeiro pedaço seria HTML e o resto apareceria com as tags à mostra.
     */
    @Test
    void messageHasNoLineBreaks() {
        assertFalse(WarningDialog.message("steam.exe", "7h 45min").contains("\n"));
    }

    @Test
    void dialogRendersTheWholeMessageAsHtml() {
        String message = WarningDialog.message("steam.exe", "7h 45min");

        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[] {"A", "B"}, "B");

        List<JLabel> labels = labelsWithText(pane);
        assertEquals(1, labels.size(), "a mensagem deve virar um único rótulo");
        assertNotNull(labels.getFirst().getClientProperty(BasicHTML.propertyKey),
                "o rótulo deve ser interpretado como HTML");
    }

    private static List<JLabel> labelsWithText(Container container) {
        List<JLabel> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel label && label.getText() != null && !label.getText().isBlank()) {
                found.add(label);
            }
            if (child instanceof Container inner) {
                found.addAll(labelsWithText(inner));
            }
        }
        return found;
    }
}
