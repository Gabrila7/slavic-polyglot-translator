package com.gabrila7.slavicpolyglot.ui;

import com.gabrila7.slavicpolyglot.model.Language;
import com.gabrila7.slavicpolyglot.model.TranslationEntry;
import com.gabrila7.slavicpolyglot.service.OfflineDictionary;
import com.gabrila7.slavicpolyglot.service.TranslationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;

/**
 * Interface principal inspirada diretamente no design e interação do Google Tradutor:
 * - Abas de idioma clicáveis com indicador azul inferior
 * - Tradução automática em tempo real enquanto você digita (debounce 350ms)
 * - Botão central circular de inverter idiomas (⇄)
 * - Duas áreas lado a lado (Origem em branco, Tradução em cinza suave)
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class MainFrame extends JFrame {

    private final TranslationService translationService = new TranslationService();

    private Language selectedSource = Language.AUTO;
    private Language selectedTarget = Language.ENGLISH;

    private final Map<Language, LanguageTabButton> sourceTabs = new EnumMap<>(Language.class);
    private final Map<Language, LanguageTabButton> targetTabs = new EnumMap<>(Language.class);

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JLabel charCountLabel;
    private JLabel detectedBadgeLabel;
    private JLabel statusFeedbackLabel;
    private Timer debounceTimer;
    private SwingWorker<TranslationEntry, Void> activeWorker;
    private boolean suppressEvents = false;

    // Cores oficiais estilo Google Translate
    private static final Color PAGE_BG = new Color(248, 249, 250);       // #F8F9FA
    private static final Color CARD_WHITE = Color.WHITE;                 // #FFFFFF
    private static final Color OUTPUT_BG = new Color(241, 243, 244);     // #F1F3F4
    private static final Color BORDER_GRAY = new Color(218, 220, 224);   // #DADCE0
    private static final Color GOOGLE_BLUE = new Color(26, 115, 232);    // #1A73E8
    private static final Color TEXT_PRIMARY = new Color(32, 33, 36);     // #202124
    private static final Color TEXT_SECONDARY = new Color(95, 99, 104);  // #5F6368

    public MainFrame() {
        super("Tradutor — Português • Inglês • Espanhol");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 560);
        setMinimumSize(new Dimension(780, 460));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(PAGE_BG);

        // 1. Barra superior estilo Google Translate
        root.add(createGoogleTopBar(), BorderLayout.NORTH);

        // 2. Container principal do cartão de tradução
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setBackground(PAGE_BG);
        centerWrapper.setBorder(new EmptyBorder(24, 28, 18, 28));

        centerWrapper.add(createMainTranslatorCard(), BorderLayout.CENTER);
        centerWrapper.add(createQuickExamplesStrip(), BorderLayout.SOUTH);

        root.add(centerWrapper, BorderLayout.CENTER);
        setContentPane(root);

        // Timer de debounce (350ms) para traduzir sozinho assim que o usuário para de digitar
        debounceTimer = new Timer(350, e -> triggerLiveTranslation());
        debounceTimer.setRepeats(false);

        updateTabSelectionUI();
        triggerLiveTranslation();
    }

    private JPanel createGoogleTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(CARD_WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY),
                new EmptyBorder(12, 28, 12, 28)
        ));

        JPanel leftBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBrand.setOpaque(false);

        JLabel iconLabel = new JLabel("🌐");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JLabel title = new JLabel("Tradutor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Português • Inglês • Espanhol");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setBorder(new EmptyBorder(2, 6, 0, 0));

        leftBrand.add(iconLabel);
        leftBrand.add(title);
        leftBrand.add(subtitle);

        statusFeedbackLabel = new JLabel("Tradução instantânea ativada");
        statusFeedbackLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusFeedbackLabel.setForeground(TEXT_SECONDARY);

        topBar.add(leftBrand, BorderLayout.WEST);
        topBar.add(statusFeedbackLabel, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createMainTranslatorCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(CARD_WHITE);
        card.setBorder(new LineBorder(BORDER_GRAY, 1, true));

        // Topo do cartão: Abas de idioma na esquerda | Botão Inverter (⇄) | Abas de idioma na direita
        card.add(createLanguageTabsHeader(), BorderLayout.NORTH);

        // Corpo do cartão: Área de digitação (Esquerda) e Área de tradução (Direita)
        JPanel splitBody = new JPanel(new GridLayout(1, 2, 0, 0));
        splitBody.setOpaque(false);

        splitBody.add(createSourcePane());
        splitBody.add(createTargetPane());

        card.add(splitBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLanguageTabsHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(CARD_WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_GRAY));

        // Abas da Esquerda (Origem: Detectar idioma, Português, Inglês, Espanhol)
        JPanel leftTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftTabs.setOpaque(false);
        leftTabs.setBorder(new EmptyBorder(0, 10, 0, 0));

        for (Language lang : new Language[]{Language.AUTO, Language.PORTUGUESE, Language.ENGLISH, Language.SPANISH}) {
            LanguageTabButton btn = new LanguageTabButton(lang.getDisplayName());
            btn.addActionListener(e -> {
                selectedSource = lang;
                updateTabSelectionUI();
                triggerLiveTranslation();
            });
            sourceTabs.put(lang, btn);
            leftTabs.add(btn);
        }

        // Centro: Botão de Inverter Idiomas (⇄)
        JPanel centerSwapPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        centerSwapPanel.setOpaque(false);

        JButton swapBtn = createIconActionButton("⇄", "Inverter idiomas (Ctrl+Shift+S)");
        swapBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        swapBtn.setPreferredSize(new Dimension(40, 36));
        swapBtn.addActionListener(e -> swapLanguages());
        centerSwapPanel.add(swapBtn);

        // Abas da Direita (Destino: Português, Inglês, Espanhol)
        JPanel rightTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rightTabs.setOpaque(false);
        rightTabs.setBorder(new EmptyBorder(0, 10, 0, 0));

        for (Language lang : Language.getTargetLanguages()) {
            LanguageTabButton btn = new LanguageTabButton(lang.getDisplayName());
            btn.addActionListener(e -> {
                selectedTarget = lang;
                updateTabSelectionUI();
                triggerLiveTranslation();
            });
            targetTabs.put(lang, btn);
            rightTabs.add(btn);
        }

        JPanel twoSides = new JPanel(new GridLayout(1, 2, 0, 0));
        twoSides.setOpaque(false);
        twoSides.add(leftTabs);
        twoSides.add(rightTabs);

        header.add(twoSides, BorderLayout.CENTER);
        header.add(centerSwapPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createSourcePane() {
        JPanel pane = new JPanel(new BorderLayout(0, 0));
        pane.setBackground(CARD_WHITE);
        pane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_GRAY));

        // Área de texto com botão "✕" no canto superior direito
        JPanel textWrapper = new JPanel(new BorderLayout());
        textWrapper.setOpaque(false);
        textWrapper.setBorder(new EmptyBorder(16, 20, 8, 12));

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        inputArea.setForeground(TEXT_PRIMARY);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(null);
        inputArea.setText("Bom dia! Como você está hoje?");

        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { onInputModified(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onInputModified(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onInputModified(); }
        });

        JScrollPane scroll = new JScrollPane(inputArea);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        // Botão X para limpar rápido (igual ao Google Translate)
        JPanel topRightClear = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRightClear.setOpaque(false);
        JButton clearXBtn = createIconActionButton("✕", "Limpar texto");
        clearXBtn.setPreferredSize(new Dimension(32, 32));
        clearXBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            detectedBadgeLabel.setText("");
            inputArea.requestFocusInWindow();
        });
        topRightClear.add(clearXBtn);

        textWrapper.add(scroll, BorderLayout.CENTER);
        textWrapper.add(topRightClear, BorderLayout.EAST);

        // Rodapé do painel esquerdo: Colar + Contador de caracteres
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(8, 20, 14, 18));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);

        JButton pasteBtn = createSubtlePillButton("📋 Colar");
        pasteBtn.addActionListener(e -> pasteFromClipboard());
        leftTools.add(pasteBtn);

        detectedBadgeLabel = new JLabel("");
        detectedBadgeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        detectedBadgeLabel.setForeground(GOOGLE_BLUE);
        leftTools.add(detectedBadgeLabel);

        charCountLabel = new JLabel("29 / 5000");
        charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        charCountLabel.setForeground(TEXT_SECONDARY);

        bottomBar.add(leftTools, BorderLayout.WEST);
        bottomBar.add(charCountLabel, BorderLayout.EAST);

        pane.add(textWrapper, BorderLayout.CENTER);
        pane.add(bottomBar, BorderLayout.SOUTH);
        return pane;
    }

    private JPanel createTargetPane() {
        JPanel pane = new JPanel(new BorderLayout(0, 0));
        pane.setBackground(OUTPUT_BG);

        JPanel textWrapper = new JPanel(new BorderLayout());
        textWrapper.setOpaque(false);
        textWrapper.setBorder(new EmptyBorder(16, 20, 8, 20));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        outputArea.setForeground(TEXT_PRIMARY);
        outputArea.setBackground(OUTPUT_BG);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setBorder(null);
        outputArea.setText("Good morning! How are you today?");

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        textWrapper.add(scroll, BorderLayout.CENTER);

        // Rodapé do painel direito: Botão Copiar Tradução
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(8, 20, 14, 20));

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton copyBtn = createSubtlePillButton("📋 Copiar tradução");
        copyBtn.addActionListener(e -> copyToClipboard(outputArea.getText()));
        rightActions.add(copyBtn);

        bottomBar.add(rightActions, BorderLayout.EAST);

        pane.add(textWrapper, BorderLayout.CENTER);
        pane.add(bottomBar, BorderLayout.SOUTH);
        return pane;
    }

    private JPanel createQuickExamplesStrip() {
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        strip.setOpaque(false);

        JLabel label = new JLabel("Testar frases rápidas:");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        strip.add(label);

        String[][] samples = {
                {"Como vai você?", "PT"},
                {"Where is the airport?", "EN"},
                {"Muchas gracias por tu ayuda", "ES"},
                {"Can you help me, please?", "EN"}
        };

        for (String[] s : samples) {
            JButton chip = createSubtlePillButton(s[0]);
            chip.addActionListener(e -> {
                selectedSource = Language.AUTO;
                if ("EN".equals(s[1])) {
                    selectedTarget = Language.PORTUGUESE;
                } else {
                    selectedTarget = Language.ENGLISH;
                }
                updateTabSelectionUI();
                inputArea.setText(s[0]);
            });
            strip.add(chip);
        }

        return strip;
    }

    private void onInputModified() {
        if (suppressEvents) return;
        String text = inputArea.getText();
        charCountLabel.setText(text.length() + " / 5000");

        if (text.trim().isEmpty()) {
            debounceTimer.stop();
            outputArea.setText("");
            detectedBadgeLabel.setText("");
            statusFeedbackLabel.setText("Digite para traduzir");
            return;
        }

        outputArea.setForeground(TEXT_SECONDARY);
        debounceTimer.restart();
    }

    private void swapLanguages() {
        Language effectiveFrom = selectedSource == Language.AUTO
                ? OfflineDictionary.guessLanguage(inputArea.getText())
                : selectedSource;

        Language oldTarget = selectedTarget;
        selectedSource = oldTarget;
        selectedTarget = effectiveFrom;

        String currentOut = outputArea.getText().trim();
        updateTabSelectionUI();

        if (!currentOut.isEmpty() && !currentOut.equals("Traduzindo...")) {
            suppressEvents = true;
            inputArea.setText(currentOut);
            charCountLabel.setText(currentOut.length() + " / 5000");
            suppressEvents = false;
        }
        triggerLiveTranslation();
    }

    private void updateTabSelectionUI() {
        for (Map.Entry<Language, LanguageTabButton> entry : sourceTabs.entrySet()) {
            entry.getValue().setSelectedTab(entry.getKey() == selectedSource);
        }
        for (Map.Entry<Language, LanguageTabButton> entry : targetTabs.entrySet()) {
            entry.getValue().setSelectedTab(entry.getKey() == selectedTarget);
        }
    }

    private void triggerLiveTranslation() {
        final String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            outputArea.setText("");
            detectedBadgeLabel.setText("");
            return;
        }

        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(true);
        }

        final Language src = selectedSource;
        final Language tgt = selectedTarget;

        statusFeedbackLabel.setText("Traduzindo...");

        activeWorker = new SwingWorker<TranslationEntry, Void>() {
            @Override
            protected TranslationEntry doInBackground() {
                return translationService.translate(text, src, tgt);
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    TranslationEntry entry = get();
                    outputArea.setForeground(TEXT_PRIMARY);
                    outputArea.setText(entry.getTranslatedText());

                    if (src == Language.AUTO) {
                        detectedBadgeLabel.setText("Idioma detectado: " + entry.getSourceLang().getDisplayName());
                    } else {
                        detectedBadgeLabel.setText("");
                    }
                    statusFeedbackLabel.setText("Traduzido automaticamente");
                } catch (Exception ex) {
                    outputArea.setForeground(TEXT_SECONDARY);
                    outputArea.setText("Sem conexão no momento.");
                    statusFeedbackLabel.setText("Modo offline");
                }
            }
        };
        activeWorker.execute();
    }

    private void copyToClipboard(String text) {
        if (text == null || text.trim().isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        statusFeedbackLabel.setText("✓ Tradução copiada para a área de transferência");
    }

    private void pasteFromClipboard() {
        try {
            String data = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data != null) {
                inputArea.setText(data);
            }
        } catch (Exception ignored) {
        }
    }

    private JButton createIconActionButton(String symbol, String tooltip) {
        JButton btn = new JButton(symbol);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btn.setForeground(TEXT_SECONDARY);
        btn.setBackground(CARD_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(GOOGLE_BLUE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(TEXT_SECONDARY);
            }
        });
        return btn;
    }

    private JButton createSubtlePillButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(TEXT_SECONDARY);
        btn.setBackground(CARD_WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Botão de aba estilo Google Translate: texto azul e barra inferior azul de 3px quando ativo.
     */
    private static class LanguageTabButton extends JButton {
        private boolean selectedTab = false;

        LanguageTabButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(new EmptyBorder(14, 14, 14, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setSelectedTab(false);
        }

        void setSelectedTab(boolean selected) {
            this.selectedTab = selected;
            setForeground(selected ? GOOGLE_BLUE : TEXT_SECONDARY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selectedTab) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(GOOGLE_BLUE);
                g2.fillRect(8, getHeight() - 3, getWidth() - 16, 3);
                g2.dispose();
            }
        }
    }
}
