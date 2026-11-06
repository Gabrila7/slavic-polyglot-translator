package com.gabrila7.slavicpolyglot;

import com.gabrila7.slavicpolyglot.ui.MainFrame;

import javax.swing.*;

/**
 * Ponto de entrada da aplicação SlavicPolyglot Desktop.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class Main {

    public static void main(String[] args) {
        // Garante codificação UTF-8 para exibição correta do alfabeto cirílico
        System.setProperty("file.encoding", "UTF-8");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Mantém o tema padrão do Swing caso o sistema não forneça tema nativo
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
