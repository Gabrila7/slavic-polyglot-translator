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
 * Layout redesenhado estilo Google Translate Mobile: o teclado virtual russo
 * inicia oculto e fica ancorado na base da tela quando ativado pelo usuário.
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
    private LinearLayout keyboardDrawer;
    private LinearLayout keyboardRowsHolder;
    private Button toggleKeyboardBtn;
    private Button translateBtn;

    private KeyboardLayout currentKeyboardLayout = KeyboardLayout.JCUKEN;
    private boolean isUppercase = false;
    private boolean isKeyboardVisible = false; // Inicia oculto para manter o visual limpo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Container raiz vertical: Topo + Área rolável + Gaveta inferior do teclado russo
        LinearLayout screenRoot = new LinearLayout(this);
        screenRoot.setOrientation(LinearLayout.VERTICAL);
        screenRoot.setBackgroundColor(Color.parseColor("#F1F5F9"));

        // 1. Barra superior fixa
        screenRoot.addView(createTopAppBar());

        // 2. Conteúdo central rolável
        ScrollView contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(true);

        LinearLayout scrollBody = new LinearLayout(this);
        scrollBody.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        scrollBody.setPadding(pad, pad, pad, pad);

        scrollBody.addView(createLanguageSelectorCard());
        scrollBody.addView(createSourceInputCard());
        scrollBody.addView(createTranslationOutputCard());
        scrollBody.addView(createQuickPhrasesCard());

        statusText = new TextView(this);
        statusText.setText("Pronto • SlavicPolyglot Mobile (@Gabrila7)");
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        statusText.setTextColor(Color.parseColor("#64748B"));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(8), 0, dp(4));
        scrollBody.addView(statusText);

        contentScroll.addView(scrollBody);
        screenRoot.addView(contentScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 3. Gaveta inferior do Teclado Russo (Inicia OCULTA / GONE)
        keyboardDrawer = createBottomKeyboardDrawer();
        keyboardDrawer.setVisibility(View.GONE);
        screenRoot.addView(keyboardDrawer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(screenRoot);
        rebuildKeyboardKeys();
    }

    private View createTopAppBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.parseColor("#0F172A"));
        bar.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("🌐 SlavicPolyglot");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView sub = new TextView(this);
        sub.setText("Inglês • Espanhol • Russo");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

        titleCol.addView(title);
        titleCol.addView(sub);

        toggleKeyboardBtn = createPillButton("⌨️ Ativar Teclado Russo", "#2563EB", "#FFFFFF");
        toggleKeyboardBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        toggleKeyboardBtn.setOnClickListener(v -> setRussianKeyboardVisible(!isKeyboardVisible));

        bar.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(toggleKeyboardBtn);
        return bar;
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
        sourceSpinner.setSelection(0); // ENGLISH por padrão

        Button swapBtn = createPillButton("⇄", "#F1F5F9", "#0F172A");
        swapBtn.setOnClickListener(v -> {
            int s = sourceSpinner.getSelectedItemPosition();
            int t = targetSpinner.getSelectedItemPosition();
            sourceSpinner.setSelection(t);
            targetSpinner.setSelection(s);
            String out = outputText.getText().toString().trim();
            if (!out.isEmpty()) {
                inputEdit.setText(out);
                performTranslation();
            }
        });

        targetSpinner = new Spinner(this);
        targetSpinner.setAdapter(adapter);
        targetSpinner.setSelection(2); // RUSSIAN por padrão

        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(sourceSpinner, spinLp);
        row.addView(swapBtn, new LinearLayout.LayoutParams(dp(44), dp(38)));
        row.addView(targetSpinner, spinLp);

        card.addView(row);

        // Chips de troca rápida de idioma
        LinearLayout presetsRow = new LinearLayout(this);
        presetsRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsRow.setPadding(0, dp(8), 0, 0);

        presetsRow.addView(createPresetChip("EN ➔ RU", 0, 2), weightParams());
        presetsRow.addView(createPresetChip("RU ➔ EN", 2, 0), weightParams());
        presetsRow.addView(createPresetChip("ES ➔ RU", 1, 2), weightParams());
        presetsRow.addView(createPresetChip("EN ➔ ES", 0, 1), weightParams());

        card.addView(presetsRow);
        return card;
    }

    private Button createPresetChip(String text, int srcIdx, int tgtIdx) {
        Button b = createPillButton(text, "#F8FAFC", "#475569");
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        b.setOnClickListener(v -> {
            sourceSpinner.setSelection(srcIdx);
            targetSpinner.setSelection(tgtIdx);
            if (!inputEdit.getText().toString().trim().isEmpty()) {
                performTranslation();
            }
        });
        return b;
    }

    private View createSourceInputCard() {
        LinearLayout card = createWhiteCard();

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("TEXTO DE ORIGEM");
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.parseColor("#64748B"));

        Button clearBtn = createPillButton("Limpar", "#F8FAFC", "#64748B");
        clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        clearBtn.setOnClickListener(v -> {
            inputEdit.setText("");
            outputText.setText("Tradução aparecerá aqui...");
            phoneticText.setText("Pronúncia: —");
        });

        topRow.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(clearBtn);

        inputEdit = new EditText(this);
        inputEdit.setText("Hello friend, how are you?");
        inputEdit.setMinLines(3);
        inputEdit.setMaxLines(5);
        inputEdit.setGravity(Gravity.TOP | Gravity.START);
        inputEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        inputEdit.setTextColor(Color.parseColor("#0F172A"));
        inputEdit.setBackgroundColor(Color.TRANSPARENT);
        inputEdit.setPadding(dp(2), dp(8), dp(2), dp(8));
        inputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String current = s.toString();
                if (TransliterationService.containsCyrillic(current)) {
                    phoneticText.setText("🔊 Pronúncia: " + TransliterationService.toLatinPhonetic(current));
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        translateBtn = createPillButton("Traduzir Agora", "#2563EB", "#FFFFFF");
        translateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        translateBtn.setOnClickListener(v -> performTranslation());

        card.addView(topRow);
        card.addView(inputEdit);
        card.addView(translateBtn);
        return card;
    }

    private View createTranslationOutputCard() {
        LinearLayout card = createWhiteCard();

        TextView label = new TextView(this);
        label.setText("TRADUÇÃO");
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(Color.parseColor("#64748B"));

        outputText = new TextView(this);
        outputText.setText("Привет друг, как дела?");
        outputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        outputText.setTextColor(Color.parseColor("#0F172A"));
        outputText.setPadding(dp(4), dp(10), dp(4), dp(10));

        phoneticText = new TextView(this);
        phoneticText.setText("🔊 Pronúncia: Privet drug, kak dela?");
        phoneticText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        phoneticText.setTextColor(Color.parseColor("#2563EB"));
        phoneticText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        phoneticText.setPadding(dp(8), dp(8), dp(8), dp(8));
        phoneticText.setBackground(roundedBg("#EFF6FF", "#BFDBFE", 8));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(8), 0, 0);

        Button copyBtn = createPillButton("📋 Copiar Tradução", "#F8FAFC", "#0F172A");
        copyBtn.setOnClickListener(v -> copyText(outputText.getText().toString(), "Tradução copiada!"));

        Button copyPhoneticBtn = createPillButton("📋 Copiar Pronúncia", "#F8FAFC", "#0F172A");
        copyPhoneticBtn.setOnClickListener(v -> copyText(phoneticText.getText().toString(), "Pronúncia copiada!"));

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
        title.setText("📖 FRASES RÁPIDAS (TOQUE PARA USAR)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#64748B"));
        title.setPadding(0, 0, 0, dp(8));
        card.addView(title);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);

        for (OfflineDictionary.PhraseRow row : OfflineDictionary.getAllPhrases()) {
            String ru = row.getByLanguage(Language.RUSSIAN);
            String pt = row.getByLanguage(Language.PORTUGUESE);
            Button chip = createPillButton(ru + " • " + pt, "#F8FAFC", "#1E293B");
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
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

    private LinearLayout createBottomKeyboardDrawer() {
        LinearLayout drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setBackgroundColor(Color.parseColor("#0F172A"));
        drawer.setPadding(dp(8), dp(10), dp(8), dp(12));

        // Cabeçalho do teclado escuro: Título + Modo + Shift + Fechar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), 0, dp(4), dp(6));

        TextView title = new TextView(this);
        title.setText("⌨️ Teclado Russo");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        title.setTextColor(Color.parseColor("#F8FAFC"));

        Button layoutBtn = createDarkKeyButton("ЙЦУКЕН", "#1E293B", "#E2E8F0");
        layoutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        layoutBtn.setOnClickListener(v -> {
            currentKeyboardLayout = (currentKeyboardLayout == KeyboardLayout.JCUKEN)
                    ? KeyboardLayout.PHONETIC
                    : KeyboardLayout.JCUKEN;
            layoutBtn.setText(currentKeyboardLayout == KeyboardLayout.JCUKEN ? "ЙЦУКЕН" : "Fonético");
            rebuildKeyboardKeys();
        });

        Button shiftBtn = createDarkKeyButton("⬆ Shift", "#1E293B", "#E2E8F0");
        shiftBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        shiftBtn.setOnClickListener(v -> {
            isUppercase = !isUppercase;
            shiftBtn.setBackground(roundedBg(isUppercase ? "#2563EB" : "#1E293B", "#334155", 6));
            rebuildKeyboardKeys();
        });

        Button closeBtn = createDarkKeyButton("✕ Fechar", "#451A26", "#FCA5A5");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        closeBtn.setOnClickListener(v -> setRussianKeyboardVisible(false));

        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(layoutBtn, marginParams(dp(4)));
        header.addView(shiftBtn, marginParams(dp(4)));
        header.addView(closeBtn);
        drawer.addView(header);

        keyboardRowsHolder = new LinearLayout(this);
        keyboardRowsHolder.setOrientation(LinearLayout.VERTICAL);
        drawer.addView(keyboardRowsHolder);

        // Barra inferior do teclado: Vírgula, Espaço, Apagar, Traduzir
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setPadding(0, dp(4), 0, 0);

        Button commaBtn = createDarkKeyButton(",", "#1E293B", "#F8FAFC");
        commaBtn.setOnClickListener(v -> insertChar(", "));

        Button spaceBtn = createDarkKeyButton("Пробел (Espaço)", "#1E293B", "#F8FAFC");
        spaceBtn.setOnClickListener(v -> insertChar(" "));

        Button backBtn = createDarkKeyButton("⌫", "#1E293B", "#FCA5A5");
        backBtn.setOnClickListener(v -> deleteLastChar());

        Button enterBtn = createDarkKeyButton("⏎ Traduzir", "#2563EB", "#FFFFFF");
        enterBtn.setOnClickListener(v -> performTranslation());

        bottomRow.addView(commaBtn, new LinearLayout.LayoutParams(0, dp(38), 0.6f));
        bottomRow.addView(spaceBtn, new LinearLayout.LayoutParams(0, dp(38), 2.2f));
        bottomRow.addView(backBtn, new LinearLayout.LayoutParams(0, dp(38), 0.7f));
        bottomRow.addView(enterBtn, new LinearLayout.LayoutParams(0, dp(38), 1.2f));

        drawer.addView(bottomRow);
        return drawer;
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
                kBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                kBtn.setTypeface(Typeface.DEFAULT_BOLD);
                kBtn.setPadding(0, 0, 0, 0);
                kBtn.setTextColor(Color.parseColor("#F8FAFC"));
                kBtn.setBackground(roundedBg("#1E293B", "#334155", 6));
                kBtn.setOnClickListener(v -> insertChar(key));

                LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(0, dp(40), 1f);
                klp.setMargins(dp(1), dp(2), dp(1), dp(2));
                rowLayout.addView(kBtn, klp);
            }
            keyboardRowsHolder.addView(rowLayout);
        }
    }

    private void setRussianKeyboardVisible(boolean visible) {
        isKeyboardVisible = visible;
        keyboardDrawer.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            toggleKeyboardBtn.setText("⌨️ Ocultar Teclado");
            toggleKeyboardBtn.setBackground(roundedBg("#16A34A", "#15803D", 8));
        } else {
            toggleKeyboardBtn.setText("⌨️ Ativar Teclado Russo");
            toggleKeyboardBtn.setBackground(roundedBg("#2563EB", "#1D4ED8", 8));
        }
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
        translateBtn.setText("Traduzindo...");
        statusText.setText("Traduzindo de " + src.getCode().toUpperCase() + " para " + tgt.getCode().toUpperCase() + "...");

        executor.execute(() -> {
            try {
                final TranslationEntry entry = translationService.translate(text, src, tgt);
                mainHandler.post(() -> {
                    outputText.setText(entry.getTranslatedText());
                    if (!entry.getTransliteration().isEmpty()) {
                        phoneticText.setText("🔊 Pronúncia: " + entry.getTransliteration());
                    } else {
                        phoneticText.setText("🔊 Pronúncia: —");
                    }
                    statusText.setText("Traduzido (" + entry.getEngineUsed() + ") às " + entry.getTimestamp());
                    translateBtn.setEnabled(true);
                    translateBtn.setText("Traduzir Agora");
                });
            } catch (final Exception e) {
                mainHandler.post(() -> {
                    outputText.setText("Não foi possível traduzir: " + e.getMessage());
                    statusText.setText("Aviso na tradução");
                    translateBtn.setEnabled(true);
                    translateBtn.setText("Traduzir Agora");
                });
            }
        });
    }

    private void copyText(String text, String msg) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("slavicpolyglot", text));
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private LinearLayout createWhiteCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int p = dp(14);
        card.setPadding(p, p, p, p);
        card.setBackground(roundedBg("#FFFFFF", "#E2E8F0", 12));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    private Button createPillButton(String text, String bgHex, String fgHex) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.parseColor(fgHex));
        b.setBackground(roundedBg(bgHex, "#CBD5E1", 8));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private Button createDarkKeyButton(String text, String bgHex, String fgHex) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(Color.parseColor(fgHex));
        b.setBackground(roundedBg(bgHex, "#334155", 6));
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        return b;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
        lp.setMargins(dp(3), dp(2), dp(3), dp(2));
        return lp;
    }

    private LinearLayout.LayoutParams marginParams(int rightMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        lp.rightMargin = rightMargin;
        return lp;
    }

    private GradientDrawable roundedBg(String fillHex, String strokeHex, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(fillHex));
        gd.setCornerRadius(dp(radiusDp));
        gd.setStroke(dp(1), Color.parseColor(strokeHex));
        return gd;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
