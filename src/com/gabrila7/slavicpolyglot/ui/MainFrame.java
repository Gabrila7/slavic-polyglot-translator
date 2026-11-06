package com.gabrila7.slavicpolyglot.ui;

import com.gabrila7.slavicpolyglot.model.Language;
import com.gabrila7.slavicpolyglot.model.TranslationEntry;
import com.gabrila7.slavicpolyglot.service.OfflineDictionary;
import com.gabrila7.slavicpolyglot.service.TranslationService;
import com.gabrila7.slavicpolyglot.service.TransliterationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Janela principal do Tradutor Poliglota (Inglês, Espanhol, Russo e Português)
 * com Teclado Virtual Russo integrado e Guia de Expressões Offline.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class MainFrame extends JFrame {

    private final TranslationService translationService = new TranslationService();

    private JComboBox<Language> sourceLangCombo;
    private JComboBox<Language> targetLangCombo;
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JTextField transliterationField;
    private JButton translateButton;
    private JButton toggleKeyboardButton;
    private JLabel statusLabel;
    private JLabel charCountLabel;
    private RussianKeyboardPanel keyboardPanel;
    private DefaultListModel<String> historyListModel;

    public MainFrame() {
        super("SlavicPolyglot - Tradutor EN / ES / RU & Teclado Cirílico");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 740);
        setMinimumSize(new Dimension(780, 620));
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(new EmptyBorder(12, 14, 10, 14));

        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("🌐 Tradutor & Teclado Russo", createTranslatorTab());
        tabbedPane.addTab("📖 Guia de Frases (EN / ES / RU)", createPhrasebookTab());
        tabbedPane.addTab("🕒 Histórico da Sessão", createHistoryTab());

        rootPanel.add(tabbedPane, BorderLayout.CENTER);
        rootPanel.add(createFooterPanel(), BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel title = new JLabel("🌐 SlavicPolyglot Desktop — Tradutor & Teclado Cirílico");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));

        JLabel subtitle = new JLabel(
                "Tradução rápida entre Inglês, Espanhol, Russo e Português com teclado virtual cirílico e fonética.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(90, 100, 115));

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTranslatorTab() {
        JPanel tabPanel = new JPanel(new BorderLayout(8, 8));
        tabPanel.setBorder(new EmptyBorder(8, 4, 4, 4));

        // 1. Barra de seleção de idiomas
        tabPanel.add(createLanguageBar(), BorderLayout.NORTH);

        // 2. Área central dividida: Entrada e Saída + Transliteração
        JPanel centerContainer = new JPanel(new BorderLayout(8, 8));

        JPanel textSplitPanel = new JPanel(new GridLayout(1, 2, 12, 0));

        // Painel de Entrada
        JPanel inputWrapper = new JPanel(new BorderLayout(4, 4));
        inputWrapper.setBorder(BorderFactory.createTitledBorder("Texto de Origem"));

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setText("Привет друг, как дела?");
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateLiveInfo(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateLiveInfo(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateLiveInfo(); }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputWrapper.add(inputScroll, BorderLayout.CENTER);

        JPanel inputActions = new JPanel(new BorderLayout());
        charCountLabel = new JLabel("Caracteres: 22");
        charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        charCountLabel.setForeground(Color.GRAY);

        JPanel inputBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton clearBtn = new JButton("Limpar");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            transliterationField.setText("");
        });

        inputBtns.add(clearBtn);
        inputActions.add(charCountLabel, BorderLayout.WEST);
        inputActions.add(inputBtns, BorderLayout.EAST);
        inputWrapper.add(inputActions, BorderLayout.SOUTH);

        // Painel de Saída
        JPanel outputWrapper = new JPanel(new BorderLayout(4, 4));
        outputWrapper.setBorder(BorderFactory.createTitledBorder("Tradução"));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(248, 250, 252));

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputWrapper.add(outputScroll, BorderLayout.CENTER);

        JPanel outputBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton copyTranslationBtn = new JButton("📋 Copiar Tradução");
        copyTranslationBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyTranslationBtn.addActionListener(e -> copyToClipboard(outputArea.getText(), "Tradução copiada!"));

        JButton useAsInputBtn = new JButton("🔄 Usar como Origem");
        useAsInputBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        useAsInputBtn.addActionListener(e -> {
            String translated = outputArea.getText().trim();
            if (!translated.isEmpty()) {
                swapLanguages();
                inputArea.setText(translated);
                executeTranslation();
            }
        });

        outputBtns.add(useAsInputBtn);
        outputBtns.add(copyTranslationBtn);
        outputWrapper.add(outputBtns, BorderLayout.SOUTH);

        textSplitPanel.add(inputWrapper);
        textSplitPanel.add(outputWrapper);

        centerContainer.add(textSplitPanel, BorderLayout.CENTER);

        // Barra de Transliteração / Pronúncia Fonética
        JPanel translitPanel = new JPanel(new BorderLayout(8, 0));
        translitPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Pronúncia Fonética / Transliteração Cirílica (Alfabeto Latino)"),
                new EmptyBorder(2, 6, 4, 6)
        ));

        transliterationField = new JTextField();
        transliterationField.setEditable(false);
        transliterationField.setFont(new Font("Consolas", Font.BOLD, 13));
        transliterationField.setForeground(new Color(30, 64, 175));
        transliterationField.setText(TransliterationService.toLatinPhonetic(inputArea.getText()));

        JButton copyPhoneticBtn = new JButton("Copiar Fonética");
        copyPhoneticBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copyPhoneticBtn.addActionListener(e -> copyToClipboard(transliterationField.getText(), "Fonética copiada!"));

        translitPanel.add(transliterationField, BorderLayout.CENTER);
        translitPanel.add(copyPhoneticBtn, BorderLayout.EAST);

        centerContainer.add(translitPanel, BorderLayout.SOUTH);
        tabPanel.add(centerContainer, BorderLayout.CENTER);

        // 3. Teclado Virtual Russo na parte inferior
        keyboardPanel = new RussianKeyboardPanel(inputArea, this::executeTranslation);
        tabPanel.add(keyboardPanel, BorderLayout.SOUTH);

        return tabPanel;
    }

    private JPanel createLanguageBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 4));
        bar.setBorder(new EmptyBorder(0, 2, 4, 2));

        JPanel selectors = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        sourceLangCombo = new JComboBox<>(Language.values());
        sourceLangCombo.setSelectedItem(Language.RUSSIAN);
        sourceLangCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton swapBtn = new JButton("⇄ Inverter Idiomas");
        swapBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        swapBtn.addActionListener(e -> swapLanguages());

        targetLangCombo = new JComboBox<>(Language.values());
        targetLangCombo.setSelectedItem(Language.ENGLISH);
        targetLangCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Botões de par rápido (EN-RU, ES-RU, EN-ES)
        JButton presetRuEn = new JButton("RU ➔ EN");
        presetRuEn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetRuEn.addActionListener(e -> setLanguagePair(Language.RUSSIAN, Language.ENGLISH));

        JButton presetEnRu = new JButton("EN ➔ RU");
        presetEnRu.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetEnRu.addActionListener(e -> setLanguagePair(Language.ENGLISH, Language.RUSSIAN));

        JButton presetEsRu = new JButton("ES ➔ RU");
        presetEsRu.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetEsRu.addActionListener(e -> setLanguagePair(Language.SPANISH, Language.RUSSIAN));

        JButton presetEnEs = new JButton("EN ➔ ES");
        presetEnEs.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetEnEs.addActionListener(e -> setLanguagePair(Language.ENGLISH, Language.SPANISH));

        selectors.add(new JLabel("De:"));
        selectors.add(sourceLangCombo);
        selectors.add(swapBtn);
        selectors.add(new JLabel("Para:"));
        selectors.add(targetLangCombo);
        selectors.add(presetRuEn);
        selectors.add(presetEnRu);
        selectors.add(presetEsRu);
        selectors.add(presetEnEs);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        toggleKeyboardButton = new JButton("⌨️ Ocultar Teclado");
        toggleKeyboardButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toggleKeyboardButton.addActionListener(e -> {
            boolean visible = !keyboardPanel.isVisible();
            keyboardPanel.setVisible(visible);
            toggleKeyboardButton.setText(visible ? "⌨️ Ocultar Teclado" : "⌨️ Mostrar Teclado Russo");
            revalidate();
        });

        translateButton = new JButton("🚀 Traduzir");
        translateButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        translateButton.addActionListener(e -> executeTranslation());

        rightActions.add(toggleKeyboardButton);
        rightActions.add(translateButton);

        bar.add(selectors, BorderLayout.WEST);
        bar.add(rightActions, BorderLayout.EAST);
        return bar;
    }

    private JPanel createPhrasebookTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel info = new JLabel(
                "Clique duas vezes em qualquer linha da tabela para enviar a frase diretamente para o Tradutor.");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(info, BorderLayout.NORTH);

        String[] columns = {"Categoria", "Inglês (EN)", "Espanhol (ES)", "Russo (RU)", "Fonética Russa", "Português (PT)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (OfflineDictionary.PhraseRow row : OfflineDictionary.getAllPhrases()) {
            String ru = row.getByLanguage(Language.RUSSIAN);
            model.addRow(new Object[]{
                    row.getCategory(),
                    row.getByLanguage(Language.ENGLISH),
                    row.getByLanguage(Language.SPANISH),
                    ru,
                    TransliterationService.toLatinPhonetic(ru),
                    row.getByLanguage(Language.PORTUGUESE)
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int r = table.getSelectedRow();
                    String ruText = (String) model.getValueAt(r, 3);
                    sourceLangCombo.setSelectedItem(Language.RUSSIAN);
                    targetLangCombo.setSelectedItem(Language.ENGLISH);
                    inputArea.setText(ruText);
                    executeTranslation();
                }
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        historyListModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyListModel);
        historyList.setFont(new Font("Consolas", Font.PLAIN, 13));

        JButton clearHistoryBtn = new JButton("Limpar Histórico");
        clearHistoryBtn.addActionListener(e -> {
            translationService.clearHistory();
            historyListModel.clear();
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Traduções realizadas nesta sessão:"), BorderLayout.WEST);
        top.add(clearHistoryBtn, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(4, 2, 0, 2));

        statusLabel = new JLabel("Status: Pronto para traduzir (Teclado Cirílico ativo)");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JLabel authorLabel = new JLabel("Desenvolvido por Gabriel Vinícius (@Gabrila7) • Ciência da Computação");
        authorLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        authorLabel.setForeground(Color.GRAY);

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(authorLabel, BorderLayout.EAST);
        return panel;
    }

    private void updateLiveInfo() {
        String text = inputArea.getText();
        charCountLabel.setText("Caracteres: " + text.length());
        if (TransliterationService.containsCyrillic(text)) {
            transliterationField.setText(TransliterationService.toLatinPhonetic(text));
        }
    }

    private void setLanguagePair(Language src, Language tgt) {
        sourceLangCombo.setSelectedItem(src);
        targetLangCombo.setSelectedItem(tgt);
    }

    private void swapLanguages() {
        Language src = (Language) sourceLangCombo.getSelectedItem();
        Language tgt = (Language) targetLangCombo.getSelectedItem();
        sourceLangCombo.setSelectedItem(tgt);
        targetLangCombo.setSelectedItem(src);
    }

    private void copyToClipboard(String content, String feedbackMsg) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
        statusLabel.setText("Status: " + feedbackMsg);
    }

    private void executeTranslation() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Digite ou insira um texto usando o teclado virtual para traduzir.",
                    "Campo Vazio", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Language src = (Language) sourceLangCombo.getSelectedItem();
        Language tgt = (Language) targetLangCombo.getSelectedItem();

        translateButton.setEnabled(false);
        statusLabel.setText("Status: Traduzindo de " + src.getDisplayName() + " para " + tgt.getDisplayName() + "...");

        SwingWorker<TranslationEntry, Void> worker = new SwingWorker<TranslationEntry, Void>() {
            @Override
            protected TranslationEntry doInBackground() {
                return translationService.translate(text, src, tgt);
            }

            @Override
            protected void done() {
                try {
                    TranslationEntry entry = get();
                    outputArea.setText(entry.getTranslatedText());
                    if (!entry.getTransliteration().isEmpty()) {
                        transliterationField.setText(entry.getTransliteration());
                    } else {
                        transliterationField.setText("— (Sem caracteres cirílicos envolvidos)");
                    }

                    historyListModel.add(0, entry.formatSummaryLine());
                    statusLabel.setText("Status: Tradução concluída via " + entry.getEngineUsed()
                            + " às " + entry.getTimestamp());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Status: Falha na tradução (" + cause.getMessage() + ")");
                    JOptionPane.showMessageDialog(MainFrame.this,
                            cause.getMessage(),
                            "Aviso de Tradução",
                            JOptionPane.WARNING_MESSAGE);
                } finally {
                    translateButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
}
