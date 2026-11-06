package com.gabrila7.slavicpolyglot.mobile;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gabrila7.slavicpolyglot.model.KeyboardLayout;
import com.gabrila7.slavicpolyglot.model.Language;
import com.gabrila7.slavicpolyglot.model.TranslationEntry;
import com.gabrila7.slavicpolyglot.service.OfflineDictionary;
import com.gabrila7.slavicpolyglot.service.TranslationService;
import com.gabrila7.slavicpolyglot.service.TransliterationService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity principal da versão Mobile (Android) do SlavicPolyglot.
 * Construída em Java nativo compartilhando o mesmo motor de tradução,
 * transliteração fonética e teclado cirílico da versão Desktop.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class MainActivity extends Activity {

    private final TranslationService translationService = new TranslationService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Spinner sourceSpinner;
    private Spinner targetSpinner;
    private EditText inputEdit;
    private TextView outputText;
    private TextView phoneticText;
    private TextView statusText;
    private LinearLayout keyboardContainer;
    private Button translateBtn;

    private KeyboardLayout currentKeyboardLayout = KeyboardLayout.JCUKEN;
    private boolean isUppercase = false;
    private boolean isKeyboardVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(Color.parseColor("#F1F5F9"));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        mainLayout.setPadding(pad, pad, pad, pad);

        // 1. Cabeçalho
        mainLayout.addView(createHeaderCard());

        // 2. Barra de seleção de idiomas
        mainLayout.addView(createLanguageSelectorCard());

        // 3. Área de entrada de texto
        mainLayout.addView(createInputCard());

        // 4. Teclado Virtual Russo (Cirílico)
        keyboardContainer = createKeyboardCard();
        mainLayout.addView(keyboardContainer);

        // 5. Resultado da Tradução + Fonética
        mainLayout.addView(createOutputCard());

        // 6. Frases rápidas (Guia de Conversação)
        mainLayout.addView(createQuickPhrasesCard());

        // 7. Rodapé
        statusText = new TextView(this);
        statusText.setText("Pronto • SlavicPolyglot Mobile (@Gabrila7)");
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusText.setTextColor(Color.parseColor("#64748B"));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(10), 0, dp(6));
        mainLayout.addView(statusText);

        rootScroll.addView(mainLayout);
        setContentView(rootScroll);

        rebuildKeyboardKeys();
    }

    private View createHeaderCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(roundedBg("#1E3A8A", 12));

        TextView title = new TextView(this);
        title.setText("🌐 SlavicPolyglot Mobile");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView sub = new TextView(this);
        sub.setText("Tradutor Inglês • Espanhol • Russo + Teclado Cirílico");
        sub.setTextColor(Color.parseColor("#BFDBFE"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        box.addView(title);
        box.addView(sub);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        box.setLayoutParams(lp);
        return box;
    }

    private View createLanguageSelectorCard() {
        LinearLayout card = createWhiteCard();

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Language[] langs = Language.values();
        ArrayAdapter<Language> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, langs);

        sourceSpinner = new Spinner(this);
        sourceSpinner.setAdapter(adapter);
        sourceSpinner.setSelection(2); // RUSSIAN

        Button swapBtn = createStyledButton("⇄", "#E2E8F0", "#1E293B");
        swapBtn.setOnClickListener(v -> {
            int s = sourceSpinner.getSelectedItemPosition();
            int t = targetSpinner.getSelectedItemPosition();
            sourceSpinner.setSelection(t);
            targetSpinner.setSelection(s);
        });

        targetSpinner = new Spinner(this);
        targetSpinner.setAdapter(adapter);
        targetSpinner.setSelection(0); // ENGLISH

        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(sourceSpinner, spinLp);
        row.addView(swapBtn, new LinearLayout.LayoutParams(dp(46), dp(40)));
        row.addView(targetSpinner, spinLp);

        card.addView(row);

        // Atalhos rápidos de pares
        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setPadding(0, dp(6), 0, 0);

        presetsRow.addView(createPresetButton("RU ➔ EN", 2, 0), weightParams());
        presetsRow.addView(createPresetButton("EN ➔ RU", 0, 2), weightParams());
        presetsRow.addView(createPresetButton("ES ➔ RU", 1, 2), weightParams());
        presetsRow.addView(createPresetButton("EN ➔ ES", 0, 1), weightParams());

        card.addView(presetsRow);
        return card;
    }

    private Button createPresetButton(String text, int srcIdx, int tgtIdx) {
        Button b = createStyledButton(text, "#EFF6FF", "#1D4ED8");
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        b.setOnClickListener(v -> {
            sourceSpinner.setSelection(srcIdx);
            targetSpinner.setSelection(tgtIdx);
        });
        return b;
    }

    private View createInputCard() {
        LinearLayout card = createWhiteCard();

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("Texto de Origem:");
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.parseColor("#334155"));

        Button toggleKbBtn = createStyledButton("⌨️ Teclado Russo", "#DBEAFE", "#1E40AF");
        toggleKbBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        toggleKbBtn.setOnClickListener(v -> {
            isKeyboardVisible = !isKeyboardVisible;
            keyboardContainer.setVisibility(isKeyboardVisible ? View.VISIBLE : View.GONE);
        });

        Button clearBtn = createStyledButton("Limpar", "#FEE2E2", "#991B1B");
        clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        clearBtn.setOnClickListener(v -> {
            inputEdit.setText("");
            outputText.setText("A tradução aparecerá aqui...");
            phoneticText.setText("Fonética: —");
        });

        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topRow.addView(label, labelLp);
        topRow.addView(toggleKbBtn);
        topRow.addView(clearBtn);

        inputEdit = new EditText(this);
        inputEdit.setText("Привет друг, как дела?");
        inputEdit.setMinLines(3);
        inputEdit.setMaxLines(5);
        inputEdit.setGravity(Gravity.TOP | Gravity.START);
        inputEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        inputEdit.setTextColor(Color.parseColor("#0F172A"));
        inputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String current = s.toString();
                if (TransliterationService.containsCyrillic(current)) {
                    phoneticText.setText("Fonética: " + TransliterationService.toLatinPhonetic(current));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        translateBtn = createStyledButton("🚀 TRADUZIR AGORA", "#2563EB", "#FFFFFF");
        translateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        translateBtn.setOnClickListener(v -> performTranslation());

        card.addView(topRow);
        card.addView(inputEdit);
        card.addView(translateBtn);
        return card;
    }

    private LinearLayout keyboardRowsHolder;

    private LinearLayout createKeyboardCard() {
        LinearLayout card = createWhiteCard();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("⌨️ Teclado Cirílico (Кириллица)");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setTextColor(Color.parseColor("#1E293B"));

        Button layoutBtn = createStyledButton("Modo: ЙЦУКЕН", "#F1F5F9", "#334155");
        layoutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        layoutBtn.setOnClickListener(v -> {
            currentKeyboardLayout = (currentKeyboardLayout == KeyboardLayout.JCUKEN)
                    ? KeyboardLayout.PHONETIC
                    : KeyboardLayout.JCUKEN;
            layoutBtn.setText(currentKeyboardLayout == KeyboardLayout.JCUKEN ? "Modo: ЙЦУКЕН" : "Modo: Fonético");
            rebuildKeyboardKeys();
        });

        Button shiftBtn = createStyledButton("⬆ Shift", "#F1F5F9", "#334155");
        shiftBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        shiftBtn.setOnClickListener(v -> {
            isUppercase = !isUppercase;
            shiftBtn.setText(isUppercase ? "⬆ SHIFT ON" : "⬆ Shift");
            rebuildKeyboardKeys();
        });

        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, tLp);
        header.addView(layoutBtn);
        header.addView(shiftBtn);
        card.addView(header);

        keyboardRowsHolder = new LinearLayout(this);
        keyboardRowsHolder.setOrientation(LinearLayout.VERTICAL);
        keyboardRowsHolder.setPadding(0, dp(6), 0, dp(4));
        card.addView(keyboardRowsHolder);

        // Linha de espaço e backspace
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        Button spaceBtn = createStyledButton("Пробел (Espaço)", "#E2E8F0", "#0F172A");
        spaceBtn.setOnClickListener(v -> insertChar(" "));

        Button backBtn = createStyledButton("⌫ Apagar", "#FEE2E2", "#991B1B");
        backBtn.setOnClickListener(v -> deleteLastChar());

        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, dp(38), 2f);
        spLp.rightMargin = dp(6);
        LinearLayout.LayoutParams bkLp = new LinearLayout.LayoutParams(0, dp(38), 1f);

        bottomRow.addView(spaceBtn, spLp);
        bottomRow.addView(backBtn, bkLp);
        card.addView(bottomRow);

        return card;
    }

    private void rebuildKeyboardKeys() {
        keyboardRowsHolder.removeAllViews();
        String[][] rows = currentKeyboardLayout.getRows(isUppercase);

        for (String[] row : rows) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);

            for (String key : row) {
                Button kBtn = new Button(this);
                kBtn.setAllCaps(false);
                kBtn.setText(key);
                kBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                kBtn.setPadding(0, 0, 0, 0);
                kBtn.setTextColor(Color.parseColor("#0F172A"));
                kBtn.setBackground(roundedBg("#F8FAFC", 6));
                kBtn.setOnClickListener(v -> insertChar(key));

                LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(0, dp(38), 1f);
                klp.setMargins(dp(1), dp(2), dp(1), dp(2));
                rowLayout.addView(kBtn, klp);
            }
            keyboardRowsHolder.addView(rowLayout);
        }
    }

    private View createOutputCard() {
        LinearLayout card = createWhiteCard();

        TextView label = new TextView(this);
        label.setText("Tradução:");
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.parseColor("#334155"));

        outputText = new TextView(this);
        outputText.setText("Toque em 'Traduzir Agora' para ver o resultado.");
        outputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        outputText.setTextColor(Color.parseColor("#0F172A"));
        outputText.setPadding(dp(8), dp(10), dp(8), dp(10));
        outputText.setBackground(roundedBg("#F8FAFC", 8));

        phoneticText = new TextView(this);
        phoneticText.setText("Fonética: " + TransliterationService.toLatinPhonetic("Привет друг, как дела?"));
        phoneticText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        phoneticText.setTextColor(Color.parseColor("#1D4ED8"));
        phoneticText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        phoneticText.setPadding(dp(4), dp(8), dp(4), dp(6));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button copyBtn = createStyledButton("📋 Copiar Tradução", "#EFF6FF", "#1D4ED8");
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("translation", outputText.getText()));
                Toast.makeText(this, "Tradução copiada!", Toast.LENGTH_SHORT).show();
            }
        });

        Button copyPhoneticBtn = createStyledButton("📋 Copiar Fonética", "#F1F5F9", "#334155");
        copyPhoneticBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("phonetic", phoneticText.getText()));
                Toast.makeText(this, "Fonética copiada!", Toast.LENGTH_SHORT).show();
            }
        });

        actionRow.addView(copyBtn, weightParams());
        actionRow.addView(copyPhoneticBtn, weightParams());

        card.addView(label);
        card.addView(outputText);
        card.addView(phoneticText);
        card.addView(actionRow);
        return card;
    }

    private View createQuickPhrasesCard() {
        LinearLayout card = createWhiteCard();

        TextView title = new TextView(this);
        title.setText("📖 Frases Rápidas Offline (Toque para testar):");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#334155"));
        title.setPadding(0, 0, 0, dp(6));
        card.addView(title);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);

        for (OfflineDictionary.PhraseRow row : OfflineDictionary.getAllPhrases()) {
            String ru = row.getByLanguage(Language.RUSSIAN);
            String en = row.getByLanguage(Language.ENGLISH);
            Button chip = createStyledButton(ru + " (" + en + ")", "#F1F5F9", "#1E293B");
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setOnClickListener(v -> {
                sourceSpinner.setSelection(2); // RU
                targetSpinner.setSelection(0); // EN
                inputEdit.setText(ru);
                performTranslation();
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            clp.rightMargin = dp(6);
            chipRow.addView(chip, clp);
        }

        hsv.addView(chipRow);
        card.addView(hsv);
        return card;
    }

    private void insertChar(String ch) {
        int start = Math.max(inputEdit.getSelectionStart(), 0);
        int end = Math.max(inputEdit.getSelectionEnd(), 0);
        inputEdit.getText().replace(Math.min(start, end), Math.max(start, end), ch, 0, ch.length());
    }

    private void deleteLastChar() {
        int start = inputEdit.getSelectionStart();
        if (start > 0) {
            inputEdit.getText().delete(start - 1, start);
        }
    }

    private void performTranslation() {
        final String text = inputEdit.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Digite um texto para traduzir.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Language src = (Language) sourceSpinner.getSelectedItem();
        final Language tgt = (Language) targetSpinner.getSelectedItem();

        translateBtn.setEnabled(false);
        statusText.setText("Traduzindo de " + src.getCode().toUpperCase() + " para " + tgt.getCode().toUpperCase() + "...");

        executor.execute(() -> {
            try {
                final TranslationEntry entry = translationService.translate(text, src, tgt);
                mainHandler.post(() -> {
                    outputText.setText(entry.getTranslatedText());
                    if (!entry.getTransliteration().isEmpty()) {
                        phoneticText.setText("Fonética: " + entry.getTransliteration());
                    } else {
                        phoneticText.setText("Fonética: —");
                    }
                    statusText.setText("Traduzido via " + entry.getEngineUsed() + " às " + entry.getTimestamp());
                    translateBtn.setEnabled(true);
                });
            } catch (final Exception e) {
                mainHandler.post(() -> {
                    outputText.setText("Erro: " + e.getMessage());
                    statusText.setText("Falha na tradução");
                    translateBtn.setEnabled(true);
                });
            }
        });
    }

    private LinearLayout createWhiteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        card.setPadding(p, p, p, p);
        card.setBackground(roundedBg("#FFFFFF", 10));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    private Button createStyledButton(String text, String bgHex, String fgHex) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(Color.parseColor(fgHex));
        b.setBackground(roundedBg(bgHex, 8));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        return b;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        return lp;
    }

    private GradientDrawable roundedBg(String hexColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(hexColor));
        gd.setCornerRadius(dp(radiusDp));
        gd.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        return gd;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
