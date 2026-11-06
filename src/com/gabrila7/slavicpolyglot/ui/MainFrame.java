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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Interface principal inspirada no Google Tradutor com suporte completo
 * a alternância entre Tema Claro e Tema Escuro (Dark Mode).
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class MainFrame extends JFrame {

    private final TranslationService translationService = new TranslationService();

    private Language selectedSource = Language.AUTO;
    private Language selectedTarget = Language.ENGLISH;
    private boolean darkMode = false;

    private final Map<Language, LanguageTabButton> sourceTabs = new EnumMap<>(Language.class);
    private final Map<Language, LanguageTabButton> targetTabs = new EnumMap<>(Language.class);
    private final List<JButton> subtleButtons = new ArrayList<>();

    private JPanel rootPanel;
    private JPanel topBar;
    private JPanel centerWrapper;
    private JPanel mainTranslatorCard;
    private JPanel tabsHeader;
    private JPanel sourcePane;
    private JPanel targetPane;

    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel sampleLabel;
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JLabel charCountLabel;
    private JLabel detectedBadgeLabel;
    private JLabel statusFeedbackLabel;
    private JButton themeToggleBtn;
    private JButton swapBtn;
    private JButton clearXBtn;

    private Timer debounceTimer;
    private SwingWorker<TranslationEntry, Void> activeWorker;
    private boolean suppressEvents = false;

    // Paleta Clara (Google Light)
    private static final Color LIGHT_PAGE_BG = new Color(248, 249, 250);      // #F8F9FA
    private static final Color LIGHT_CARD = Color.WHITE;                      // #FFFFFF
    private static final Color LIGHT_OUTPUT_BG = new Color(241, 243, 244);    // #F1F3F4
    private static final Color LIGHT_BORDER = new Color(218, 220, 224);       // #DADCE0
    private static final Color LIGHT_BLUE = new Color(26, 115, 232);          // #1A73E8
    private static final Color LIGHT_TEXT_PRI = new Color(32, 33, 36);        // #202124
    private static final Color LIGHT_TEXT_SEC = new Color(95, 99, 104);       // #5F6368

    // Paleta Escura (Google Dark)
    private static final Color DARK_PAGE_BG = new Color(15, 23, 42);          // #0F172A
    private static final Color DARK_CARD = new Color(30, 41, 59);             // #1E293B
    private static final Color DARK_OUTPUT_BG = new Color(23, 32, 48);        // #172030
    private static final Color DARK_BORDER = new Color(51, 65, 85);           // #334155
    private static final Color DARK_BLUE = new Color(138, 180, 248);          // #8AB4F8
    private static final Color DARK_TEXT_PRI = new Color(241, 245, 249);      // #F1F5F9
    private static final Color DARK_TEXT_SEC = new Color(148, 163, 184);      // #94A3B8

    public MainFrame() {
        super("Tradutor — Português • Inglês • Espanhol");
        initUI();
    }

    private Color getPageBg() { return darkMode ? DARK_PAGE_BG : LIGHT_PAGE_BG; }
    private Color getCardBg() { return darkMode ? DARK_CARD : LIGHT_CARD; }
    private Color getOutputBg() { return darkMode ? DARK_OUTPUT_BG : LIGHT_OUTPUT_BG; }
    private Color getBorderColor() { return darkMode ? DARK_BORDER : LIGHT_BORDER; }
    private Color getAccentBlue() { return darkMode ? DARK_BLUE : LIGHT_BLUE; }
    private Color getTextPrimary() { return darkMode ? DARK_TEXT_PRI : LIGHT_TEXT_PRI; }
    private Color getTextSecondary() { return darkMode ? DARK_TEXT_SEC : LIGHT_TEXT_SEC; }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 560);
        setMinimumSize(new Dimension(780, 460));
        setLocationRelativeTo(null);

        rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.add(createGoogleTopBar(), BorderLayout.NORTH);

        centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setBorder(new EmptyBorder(24, 28, 18, 28));

        centerWrapper.add(createMainTranslatorCard(), BorderLayout.CENTER);
        centerWrapper.add(createQuickExamplesStrip(), BorderLayout.SOUTH);

        rootPanel.add(centerWrapper, BorderLayout.CENTER);
        setContentPane(rootPanel);

        debounceTimer = new Timer(350, e -> triggerLiveTranslation());
        debounceTimer.setRepeats(false);

        applyTheme();
        triggerLiveTranslation();
    }

    private JPanel createGoogleTopBar() {
        topBar = new JPanel(new BorderLayout());

        JPanel leftBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftBrand.setOpaque(false);

        JLabel iconLabel = new JLabel("🌐");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        titleLabel = new JLabel("Tradutor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        subtitleLabel = new JLabel("Português • Inglês • Espanhol");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setBorder(new EmptyBorder(2, 6, 0, 0));

        leftBrand.add(iconLabel);
        leftBrand.add(titleLabel);
        leftBrand.add(subtitleLabel);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightControls.setOpaque(false);

        statusFeedbackLabel = new JLabel("Tradução instantânea ativada");
        statusFeedbackLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        themeToggleBtn = createSubtlePillButton("🌙 Tema Escuro");
        themeToggleBtn.addActionListener(e -> {
            darkMode = !darkMode;
            applyTheme();
        });

        rightControls.add(statusFeedbackLabel);
        rightControls.add(themeToggleBtn);

        topBar.add(leftBrand, BorderLayout.WEST);
        topBar.add(rightControls, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createMainTranslatorCard() {
        mainTranslatorCard = new JPanel(new BorderLayout(0, 0));
        mainTranslatorCard.add(createLanguageTabsHeader(), BorderLayout.NORTH);

        JPanel splitBody = new JPanel(new GridLayout(1, 2, 0, 0));
        splitBody.setOpaque(false);

        splitBody.add(createSourcePane());
        splitBody.add(createTargetPane());

        mainTranslatorCard.add(splitBody, BorderLayout.CENTER);
        return mainTranslatorCard;
    }

    private JPanel createLanguageTabsHeader() {
        tabsHeader = new JPanel(new BorderLayout(0, 0));

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

        JPanel centerSwapPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        centerSwapPanel.setOpaque(false);

        swapBtn = createIconActionButton("⇄", "Inverter idiomas");
        swapBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        swapBtn.setPreferredSize(new Dimension(40, 36));
        swapBtn.addActionListener(e -> swapLanguages());
        centerSwapPanel.add(swapBtn);

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

        tabsHeader.add(twoSides, BorderLayout.CENTER);
        tabsHeader.add(centerSwapPanel, BorderLayout.EAST);
        return tabsHeader;
    }

    private JPanel createSourcePane() {
        sourcePane = new JPanel(new BorderLayout(0, 0));

        JPanel textWrapper = new JPanel(new BorderLayout());
        textWrapper.setOpaque(false);
        textWrapper.setBorder(new EmptyBorder(16, 20, 8, 12));

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 22));
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

        JPanel topRightClear = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topRightClear.setOpaque(false);
        clearXBtn = createIconActionButton("✕", "Limpar texto");
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
        leftTools.add(detectedBadgeLabel);

        charCountLabel = new JLabel("29 / 5000");
        charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bottomBar.add(leftTools, BorderLayout.WEST);
        bottomBar.add(charCountLabel, BorderLayout.EAST);

        sourcePane.add(textWrapper, BorderLayout.CENTER);
        sourcePane.add(bottomBar, BorderLayout.SOUTH);
        return sourcePane;
    }

    private JPanel createTargetPane() {
        targetPane = new JPanel(new BorderLayout(0, 0));

        JPanel textWrapper = new JPanel(new BorderLayout());
        textWrapper.setOpaque(false);
        textWrapper.setBorder(new EmptyBorder(16, 20, 8, 20));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 22));
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

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(8, 20, 14, 20));

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton copyBtn = createSubtlePillButton("📋 Copiar tradução");
        copyBtn.addActionListener(e -> copyToClipboard(outputArea.getText()));
        rightActions.add(copyBtn);

        bottomBar.add(rightActions, BorderLayout.EAST);

        targetPane.add(textWrapper, BorderLayout.CENTER);
        targetPane.add(bottomBar, BorderLayout.SOUTH);
        return targetPane;
    }

    private JPanel createQuickExamplesStrip() {
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        strip.setOpaque(false);

        sampleLabel = new JLabel("Testar frases rápidas:");
        sampleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        strip.add(sampleLabel);

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
                selectedTarget = "EN".equals(s[1]) ? Language.PORTUGUESE : Language.ENGLISH;
                updateTabSelectionUI();
                inputArea.setText(s[0]);
            });
            strip.add(chip);
        }

        return strip;
    }

    private void applyTheme() {
        Color pageBg = getPageBg();
        Color cardBg = getCardBg();
        Color outBg = getOutputBg();
        Color border = getBorderColor();
        Color pri = getTextPrimary();
        Color sec = getTextSecondary();
        Color blue = getAccentBlue();

        rootPanel.setBackground(pageBg);
        centerWrapper.setBackground(pageBg);

        topBar.setBackground(cardBg);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, border),
                new EmptyBorder(12, 28, 12, 28)
        ));

        titleLabel.setForeground(pri);
        subtitleLabel.setForeground(sec);
        statusFeedbackLabel.setForeground(sec);
        sampleLabel.setForeground(sec);

        themeToggleBtn.setText(darkMode ? "☀️ Tema Claro" : "🌙 Tema Escuro");

        mainTranslatorCard.setBackground(cardBg);
        mainTranslatorCard.setBorder(new LineBorder(border, 1, true));

        tabsHeader.setBackground(cardBg);
        tabsHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border));

        sourcePane.setBackground(cardBg);
        sourcePane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, border));

        targetPane.setBackground(outBg);

        inputArea.setBackground(cardBg);
        inputArea.setForeground(pri);
        inputArea.setCaretColor(pri);

        outputArea.setBackground(outBg);
        outputArea.setForeground(pri);

        detectedBadgeLabel.setForeground(blue);
        charCountLabel.setForeground(sec);

        swapBtn.setForeground(sec);
        clearXBtn.setForeground(sec);

        for (JButton btn : subtleButtons) {
            btn.setBackground(cardBg);
            btn.setForeground(pri);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(border, 1, true),
                    new EmptyBorder(5, 12, 5, 12)
            ));
        }

        updateTabSelectionUI();
        repaint();
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

        outputArea.setForeground(getTextSecondary());
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
                    outputArea.setForeground(getTextPrimary());
                    outputArea.setText(entry.getTranslatedText());

                    if (src == Language.AUTO) {
                        detectedBadgeLabel.setText("Idioma detectado: " + entry.getSourceLang().getDisplayName());
                    } else {
                        detectedBadgeLabel.setText("");
                    }
                    statusFeedbackLabel.setText("Traduzido automaticamente");
                } catch (Exception ex) {
                    outputArea.setForeground(getTextSecondary());
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
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(getAccentBlue());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(getTextSecondary());
            }
        });
        return btn;
    }

    private JButton createSubtlePillButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        subtleButtons.add(btn);
        return btn;
    }

    private class LanguageTabButton extends JButton {
        private boolean selectedTab = false;

        LanguageTabButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(new EmptyBorder(14, 14, 14, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setSelectedTab(boolean selected) {
            this.selectedTab = selected;
            setForeground(selected ? getAccentBlue() : getTextSecondary());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selectedTab) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getAccentBlue());
                g2.fillRect(8, getHeight() - 3, getWidth() - 16, 3);
                g2.dispose();
            }
        }
    }
}
