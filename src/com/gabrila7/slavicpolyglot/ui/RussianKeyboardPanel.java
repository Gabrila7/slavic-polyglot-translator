package com.gabrila7.slavicpolyglot.ui;

import com.gabrila7.slavicpolyglot.model.KeyboardLayout;
import com.gabrila7.slavicpolyglot.service.TransliterationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Componente de teclado virtual interativo de letras russas (Cirílico).
 * Permite alternar entre os layouts ЙЦУКЕН (padrão russo) e Fonético,
 * ativar Shift/Caps e visualizar a dica de som latino de cada letra.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class RussianKeyboardPanel extends JPanel {

    private final JTextArea targetTextArea;
    private final Runnable onEnterCallback;

    private KeyboardLayout currentLayout = KeyboardLayout.JCUKEN;
    private boolean uppercase = false;

    private final JPanel keysContainer;
    private final JComboBox<KeyboardLayout> layoutCombo;
    private final JToggleButton shiftToggle;
    private final List<JButton> letterButtons = new ArrayList<>();

    private static final Color KEY_BG = new Color(245, 247, 250);
    private static final Color KEY_BORDER = new Color(205, 212, 222);
    private static final Color ACCENT_BLUE = new Color(37, 99, 235);

    public RussianKeyboardPanel(JTextArea targetTextArea, Runnable onEnterCallback) {
        this.targetTextArea = targetTextArea;
        this.onEnterCallback = onEnterCallback;

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("⌨️ Teclado Virtual Russo (Кириллица)"),
                new EmptyBorder(6, 8, 8, 8)
        ));

        // Barra superior do teclado: Seletor de layout + Dica + Shift
        JPanel topBar = new JPanel(new BorderLayout(10, 0));

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JLabel layoutLabel = new JLabel("Layout:");
        layoutLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        layoutCombo = new JComboBox<>(KeyboardLayout.values());
        layoutCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        layoutCombo.addActionListener(e -> {
            currentLayout = (KeyboardLayout) layoutCombo.getSelectedItem();
            rebuildKeyGrid();
        });

        shiftToggle = new JToggleButton("⬆ MAIÚSCULAS (Shift)");
        shiftToggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        shiftToggle.setFocusPainted(false);
        shiftToggle.addActionListener(e -> {
            uppercase = shiftToggle.isSelected();
            rebuildKeyGrid();
        });

        leftControls.add(layoutLabel);
        leftControls.add(layoutCombo);
        leftControls.add(shiftToggle);

        JLabel phoneticHint = new JLabel("Dica: passe o mouse sobre uma tecla para ver o som em alfabeto latino");
        phoneticHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        phoneticHint.setForeground(new Color(100, 110, 125));

        topBar.add(leftControls, BorderLayout.WEST);
        topBar.add(phoneticHint, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Grade de teclas
        keysContainer = new JPanel();
        keysContainer.setLayout(new BoxLayout(keysContainer, BoxLayout.Y_AXIS));
        add(keysContainer, BorderLayout.CENTER);

        // Barra inferior: Espaço, Backspace, Limpar, Traduzir
        add(createActionRow(), BorderLayout.SOUTH);

        rebuildKeyGrid();
    }

    private void rebuildKeyGrid() {
        keysContainer.removeAll();
        letterButtons.clear();

        String[][] rows = currentLayout.getRows(uppercase);
        for (String[] row : rows) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
            for (String keyChar : row) {
                JButton keyBtn = createKeyButton(keyChar);
                letterButtons.add(keyBtn);
                rowPanel.add(keyBtn);
            }
            keysContainer.add(rowPanel);
        }

        keysContainer.revalidate();
        keysContainer.repaint();
    }

    private JButton createKeyButton(String keyChar) {
        String sound = TransliterationService.toLatinPhonetic(keyChar);
        String labelHtml;
        if (!sound.isEmpty() && !sound.equalsIgnoreCase(keyChar)) {
            labelHtml = String.format(
                    "<html><center><b><font size='4'>%s</font></b><br><font size='2' color='#64748B'>%s</font></center></html>",
                    keyChar, sound);
        } else {
            labelHtml = String.format("<html><center><b><font size='4'>%s</font></b></center></html>", keyChar);
        }

        JButton btn = new JButton(labelHtml);
        btn.setPreferredSize(new Dimension(48, 42));
        btn.setFocusPainted(false);
        btn.setBackground(KEY_BG);
        btn.setBorder(new LineBorder(KEY_BORDER, 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (!sound.isEmpty()) {
            btn.setToolTipText("Letra '" + keyChar + "' • Pronúncia aproximada: [" + sound + "]");
        }

        btn.addActionListener(e -> insertAtCaret(keyChar));
        return btn;
    }

    private JPanel createActionRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));

        JButton commaBtn = createActionKey(",", 45);
        commaBtn.addActionListener(e -> insertAtCaret(", "));

        JButton spaceBtn = createActionKey("Пробел (Espaço)", 240);
        spaceBtn.addActionListener(e -> insertAtCaret(" "));

        JButton backspaceBtn = createActionKey("⌫ Apagar", 95);
        backspaceBtn.addActionListener(e -> deleteBeforeCaret());

        JButton clearBtn = createActionKey("🗑 Limpar", 90);
        clearBtn.addActionListener(e -> {
            targetTextArea.setText("");
            targetTextArea.requestFocusInWindow();
        });

        JButton enterBtn = createActionKey("⏎ Traduzir Agora", 140);
        enterBtn.setBackground(ACCENT_BLUE);
        enterBtn.setForeground(Color.WHITE);
        enterBtn.addActionListener(e -> {
            if (onEnterCallback != null) {
                onEnterCallback.run();
            }
        });

        row.add(commaBtn);
        row.add(spaceBtn);
        row.add(backspaceBtn);
        row.add(clearBtn);
        row.add(enterBtn);
        return row;
    }

    private JButton createActionKey(String text, int width) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(width, 34));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void insertAtCaret(String text) {
        int pos = targetTextArea.getCaretPosition();
        targetTextArea.insert(text, pos);
        targetTextArea.requestFocusInWindow();
    }

    private void deleteBeforeCaret() {
        String current = targetTextArea.getText();
        int pos = targetTextArea.getCaretPosition();
        if (pos > 0 && !current.isEmpty()) {
            targetTextArea.replaceRange("", pos - 1, pos);
        }
        targetTextArea.requestFocusInWindow();
    }
}
