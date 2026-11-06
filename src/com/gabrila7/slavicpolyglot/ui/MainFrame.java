package com.gabrila7.slavicpolyglot.ui;

import com.gabrila7.slavicpolyglot.model.Language;
import com.gabrila7.slavicpolyglot.model.TranslationEntry;
import com.gabrila7.slavicpolyglot.service.OfflineDictionary;
import com.gabrila7.slavicpolyglot.service.TranslationService;
import com.gabrila7.slavicpolyglot.service.TransliterationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Janela principal do SlavicPolyglot redesenhada com layout limpo estilo
 * DeepL / Google Translate. O teclado virtual russo fica recolhido por padrão
 * e pode ser ativado sob demanda em um clique.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class MainFrame extends JFrame {

    private final TranslationService translationService = new TranslationService();

    private JComboBox<Language> sourceLangCombo;
    private JComboBox<Language> targetLangCombo;
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JLabel phoneticValueLabel;
    private JLabel charCountLabel;
    private JLabel statusLabel;
    private JButton translateButton;
    private JButton toggleKeyboardButton;
    private RussianKeyboardPanel keyboardDrawer;

    // Paleta moderna clara com cabeçalho escuro
    private static final Color APP_BG = new Color(241, 245, 249);         // #F1F5F9
    private static final Color CARD_BG = Color.WHITE;                     // #FFFFFF
    private static final Color CARD_BORDER = new Color(226, 232, 240);    // #E2E8F0
    private static final Color HEADER_BG = new Color(15, 23, 42);         // #0F172A
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);     // #2563EB
    private static final Color TEXT_DARK = new Color(15, 23, 42);        // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);     // #64748B

    public MainFrame() {
        super("SlavicPolyglot — Tradutor Inglês • Espanhol • Russo");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 640);
        setMinimumSize(new Dimension(800, 540));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(APP_BG);

        // 1. Topo escuro elegante com título e botões de utilidades
        root.add(createTopHeaderBar(), BorderLayout.NORTH);

        // 2. Corpo central (Seletor de idiomas + Cartões lado a lado + Gaveta do teclado oculta)
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(APP_BG);
        body.setBorder(new EmptyBorder(14, 18, 12, 18));

        body.add(createLanguageControlBar(), BorderLayout.NORTH);
        body.add(createTranslationCardsArea(), BorderLayout.CENTER);

        // Gaveta do teclado virtual russo — inicia OCULTA para não poluir a tela
        keyboardDrawer = new RussianKeyboardPanel(
                inputArea,
                this::executeTranslation,
                () -> setKeyboardDrawerVisible(false)
        );
        keyboardDrawer.setVisible(false);
        body.add(keyboardDrawer, BorderLayout.SOUTH);

        root.add(body, BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        setupKeyboardShortcuts();
    }

    private JPanel createTopHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Marca e subtítulo
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandBox.setOpaque(false);

        JLabel title = new JLabel("🌐 SlavicPolyglot");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel badge = new JLabel("EN • ES • RU • PT");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(new Color(147, 197, 253));
        badge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(59, 130, 246), 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));

        brandBox.add(title);
        brandBox.add(badge);

        // Botões do topo: Guia de Frases e Ativar Teclado Russo
        JPanel actionsBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsBox.setOpaque(false);

        JButton phrasebookBtn = createHeaderButton("📖 Frases Prontas", new Color(30, 41, 59), Color.WHITE);
        phrasebookBtn.setToolTipText("Abrir lista de expressões prontas em Inglês, Espanhol e Russo");
        phrasebookBtn.addActionListener(e -> showPhrasebookDialog());

        toggleKeyboardButton = createHeaderButton("⌨️ Ativar Teclado Russo", PRIMARY_BLUE, Color.WHITE);
        toggleKeyboardButton.setToolTipText("Mostrar ou ocultar o teclado virtual de letras russas (Cirílico)");
        toggleKeyboardButton.addActionListener(e -> setKeyboardDrawerVisible(!keyboardDrawer.isVisible()));

        actionsBox.add(phrasebookBtn);
        actionsBox.add(toggleKeyboardButton);

        header.add(brandBox, BorderLayout.WEST);
        header.add(actionsBox, BorderLayout.EAST);
        return header;
    }

    private JPanel createLanguageControlBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        // Seletores de idioma à esquerda
        JPanel leftSelectors = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSelectors.setOpaque(false);

        JLabel fromLabel = new JLabel("Traduzir de:");
        fromLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fromLabel.setForeground(TEXT_MUTED);

        sourceLangCombo = new JComboBox<>(Language.values());
        sourceLangCombo.setSelectedItem(Language.ENGLISH);
        sourceLangCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sourceLangCombo.setPreferredSize(new Dimension(175, 34));

        JButton swapBtn = createFlatButton("⇄ Inverter", new Color(241, 245, 249), TEXT_DARK);
        swapBtn.setToolTipText("Inverter idiomas e texto");
        swapBtn.addActionListener(e -> swapLanguagesAndText());

        JLabel toLabel = new JLabel("Para:");
        toLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toLabel.setForeground(TEXT_MUTED);

        targetLangCombo = new JComboBox<>(Language.values());
        targetLangCombo.setSelectedItem(Language.RUSSIAN);
        targetLangCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        targetLangCombo.setPreferredSize(new Dimension(175, 34));

        leftSelectors.add(fromLabel);
        leftSelectors.add(sourceLangCombo);
        leftSelectors.add(swapBtn);
        leftSelectors.add(toLabel);
        leftSelectors.add(targetLangCombo);

        // Pares rápidos + Botão Traduzir à direita
        JPanel rightQuick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightQuick.setOpaque(false);

        JButton btnEnRu = createChipButton("EN ➔ RU", Language.ENGLISH, Language.RUSSIAN);
        JButton btnRuEn = createChipButton("RU ➔ EN", Language.RUSSIAN, Language.ENGLISH);
        JButton btnEsRu = createChipButton("ES ➔ RU", Language.SPANISH, Language.RUSSIAN);
        JButton btnEnEs = createChipButton("EN ➔ ES", Language.ENGLISH, Language.SPANISH);

        translateButton = createFlatButton("Traduzir Agora (Ctrl+Enter)", PRIMARY_BLUE, Color.WHITE);
        translateButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        translateButton.setPreferredSize(new Dimension(195, 34));
        translateButton.addActionListener(e -> executeTranslation());

        rightQuick.add(btnEnRu);
        rightQuick.add(btnRuEn);
        rightQuick.add(btnEsRu);
        rightQuick.add(btnEnEs);
        rightQuick.add(translateButton);

        bar.add(leftSelectors, BorderLayout.WEST);
        bar.add(rightQuick, BorderLayout.EAST);
        return bar;
    }

    private JPanel createTranslationCardsArea() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);

        // Cartão Esquerdo: Texto de Origem
        JPanel sourceCard = new JPanel(new BorderLayout(0, 8));
        sourceCard.setBackground(CARD_BG);
        sourceCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(12, 14, 10, 14)
        ));

        JPanel sourceTop = new JPanel(new BorderLayout());
        sourceTop.setOpaque(false);
        JLabel sourceTitle = new JLabel("TEXTO DE ORIGEM");
        sourceTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sourceTitle.setForeground(TEXT_MUTED);

        JLabel kbShortcutHint = new JLabel("Precisa digitar em russo? Clique em '⌨️ Ativar Teclado Russo' no topo");
        kbShortcutHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        kbShortcutHint.setForeground(PRIMARY_BLUE);
        kbShortcutHint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        kbShortcutHint.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                setKeyboardDrawerVisible(!keyboardDrawer.isVisible());
            }
        });

        sourceTop.add(sourceTitle, BorderLayout.WEST);
        sourceTop.add(kbShortcutHint, BorderLayout.EAST);

        inputArea = new JTextArea();
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        inputArea.setForeground(TEXT_DARK);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(6, 2, 6, 2));
        inputArea.setText("Hello friend, how are you?");
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { onInputTextChanged(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onInputTextChanged(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onInputTextChanged(); }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);

        JPanel sourceBottom = new JPanel(new BorderLayout());
        sourceBottom.setOpaque(false);
        sourceBottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(241, 245, 249)));

        charCountLabel = new JLabel("26 caracteres");
        charCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        charCountLabel.setForeground(TEXT_MUTED);

        JButton clearBtn = createFlatButton("Limpar texto", new Color(248, 250, 252), TEXT_MUTED);
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
            phoneticValueLabel.setText("Digite ou traduza uma frase em russo para ver como pronunciar.");
            inputArea.requestFocusInWindow();
        });

        sourceBottom.add(charCountLabel, BorderLayout.WEST);
        sourceBottom.add(clearBtn, BorderLayout.EAST);

        sourceCard.add(sourceTop, BorderLayout.NORTH);
        sourceCard.add(inputScroll, BorderLayout.CENTER);
        sourceCard.add(sourceBottom, BorderLayout.SOUTH);

        // Cartão Direito: Resultado da Tradução
        JPanel targetCard = new JPanel(new BorderLayout(0, 8));
        targetCard.setBackground(new Color(248, 250, 252));
        targetCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(12, 14, 10, 14)
        ));

        JPanel targetTop = new JPanel(new BorderLayout());
        targetTop.setOpaque(false);
        JLabel targetTitle = new JLabel("TRADUÇÃO");
        targetTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        targetTitle.setForeground(TEXT_MUTED);
        targetTop.add(targetTitle, BorderLayout.WEST);

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        outputArea.setForeground(TEXT_DARK);
        outputArea.setBackground(new Color(248, 250, 252));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setBorder(new EmptyBorder(6, 2, 6, 2));
        outputArea.setText("Привет друг, как дела?");

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(null);

        JPanel targetBottom = new JPanel(new BorderLayout());
        targetBottom.setOpaque(false);
        targetBottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));

        JPanel targetActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        targetActions.setOpaque(false);

        JButton copyBtn = createFlatButton("📋 Copiar Tradução", new Color(239, 246, 255), PRIMARY_BLUE);
        copyBtn.addActionListener(e -> copyToClipboard(outputArea.getText(), "Tradução copiada para a área de transferência!"));

        targetActions.add(copyBtn);
        targetBottom.add(targetActions, BorderLayout.EAST);

        targetCard.add(targetTop, BorderLayout.NORTH);
        targetCard.add(outputScroll, BorderLayout.CENTER);
        targetCard.add(targetBottom, BorderLayout.SOUTH);

        grid.add(sourceCard);
        grid.add(targetCard);

        // Faixa inferior de Pronúncia Fonética (Transliteração)
        JPanel phoneticBar = new JPanel(new BorderLayout(10, 0));
        phoneticBar.setBackground(CARD_BG);
        phoneticBar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JLabel phoneticTitle = new JLabel("🔊 Pronúncia Russa (Alfabeto Latino):");
        phoneticTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        phoneticTitle.setForeground(TEXT_MUTED);

        phoneticValueLabel = new JLabel("Privet drug, kak dela?");
        phoneticValueLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        phoneticValueLabel.setForeground(PRIMARY_BLUE);

        JButton copyPhoneticBtn = createFlatButton("Copiar Pronúncia", new Color(241, 245, 249), TEXT_DARK);
        copyPhoneticBtn.addActionListener(e -> copyToClipboard(phoneticValueLabel.getText(), "Pronúncia fonética copiada!"));

        phoneticBar.add(phoneticTitle, BorderLayout.WEST);
        phoneticBar.add(phoneticValueLabel, BorderLayout.CENTER);
        phoneticBar.add(copyPhoneticBtn, BorderLayout.EAST);

        wrapper.add(grid, BorderLayout.CENTER);
        wrapper.add(phoneticBar, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER),
                new EmptyBorder(6, 18, 6, 18)
        ));

        statusLabel = new JLabel("Pronto • Dica: pressione Ctrl+Enter para traduzir rapidamente");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);

        JLabel author = new JLabel("Gabriel Vinícius (@Gabrila7)");
        author.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        author.setForeground(TEXT_MUTED);

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(author, BorderLayout.EAST);
        return bar;
    }

    private void setKeyboardDrawerVisible(boolean visible) {
        keyboardDrawer.setVisible(visible);
        if (visible) {
            toggleKeyboardButton.setText("⌨️ Ocultar Teclado Russo");
            toggleKeyboardButton.setBackground(new Color(22, 163, 74)); // Verde indicando ativo
            statusLabel.setText("Teclado Virtual Russo ativado • Clique nas teclas abaixo para inserir letras cirílicas");
        } else {
            toggleKeyboardButton.setText("⌨️ Ativar Teclado Russo");
            toggleKeyboardButton.setBackground(PRIMARY_BLUE);
            statusLabel.setText("Teclado Virtual Russo recolhido");
        }
        revalidate();
        repaint();
    }

    private void showPhrasebookDialog() {
        JDialog dialog = new JDialog(this, "📖 Guia Rápido de Frases (Clique duas vezes para usar)", true);
        dialog.setSize(760, 460);
        dialog.setLocationRelativeTo(this);

        String[] cols = {"Categoria", "Inglês (EN)", "Espanhol (ES)", "Russo (RU)", "Pronúncia Russa"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        for (OfflineDictionary.PhraseRow row : OfflineDictionary.getAllPhrases()) {
            String ru = row.getByLanguage(Language.RUSSIAN);
            model.addRow(new Object[]{
                    row.getCategory(),
                    row.getByLanguage(Language.ENGLISH),
                    row.getByLanguage(Language.SPANISH),
                    ru,
                    TransliterationService.toLatinPhonetic(ru)
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(28);
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
                    dialog.dispose();
                    executeTranslation();
                }
            }
        });

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel hint = new JLabel("Dê dois cliques em qualquer frase abaixo para carregá-la no tradutor:");
        hint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        content.add(hint, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void setupKeyboardShortcuts() {
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputArea.getInputMap().put(ctrlEnter, "translateNow");
        inputArea.getActionMap().put("translateNow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeTranslation();
            }
        });
    }

    private void onInputTextChanged() {
        String text = inputArea.getText();
        charCountLabel.setText(text.length() + " caracteres");
        if (TransliterationService.containsCyrillic(text)) {
            phoneticValueLabel.setText(TransliterationService.toLatinPhonetic(text));
        }
    }

    private void swapLanguagesAndText() {
        Language src = (Language) sourceLangCombo.getSelectedItem();
        Language tgt = (Language) targetLangCombo.getSelectedItem();
        sourceLangCombo.setSelectedItem(tgt);
        targetLangCombo.setSelectedItem(src);

        String currentOut = outputArea.getText().trim();
        if (!currentOut.isEmpty()) {
            inputArea.setText(currentOut);
            executeTranslation();
        }
    }

    private void executeTranslation() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("Digite algum texto no quadro da esquerda para traduzir.");
            inputArea.requestFocusInWindow();
            return;
        }

        Language src = (Language) sourceLangCombo.getSelectedItem();
        Language tgt = (Language) targetLangCombo.getSelectedItem();

        translateButton.setEnabled(false);
        translateButton.setText("Traduzindo...");
        statusLabel.setText("Traduzindo de " + src.getDisplayName() + " para " + tgt.getDisplayName() + "...");

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
                        phoneticValueLabel.setText(entry.getTransliteration());
                    } else {
                        phoneticValueLabel.setText("— (Apenas textos em Russo possuem transcrição fonética)");
                    }
                    statusLabel.setText("Tradução concluída (" + entry.getEngineUsed() + ") às " + entry.getTimestamp());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Aviso: " + cause.getMessage());
                    outputArea.setText("Não foi possível traduzir agora.\n" + cause.getMessage());
                } finally {
                    translateButton.setEnabled(true);
                    translateButton.setText("Traduzir Agora (Ctrl+Enter)");
                }
            }
        };

        worker.execute();
    }

    private void copyToClipboard(String text, String message) {
        if (text == null || text.trim().isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        statusLabel.setText(message);
    }

    private JButton createHeaderButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(71, 85, 105), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createFlatButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createChipButton(String label, Language src, Language tgt) {
        JButton btn = createFlatButton(label, new Color(248, 250, 252), TEXT_MUTED);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.addActionListener(e -> {
            sourceLangCombo.setSelectedItem(src);
            targetLangCombo.setSelectedItem(tgt);
            if (!inputArea.getText().trim().isEmpty()) {
                executeTranslation();
            }
        });
        return btn;
    }
}
