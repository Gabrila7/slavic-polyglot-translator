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

import com.gabrila7.slavicpolyglot.model.Language;
import com.gabrila7.slavicpolyglot.model.TranslationEntry;
import com.gabrila7.slavicpolyglot.service.OfflineDictionary;
import com.gabrila7.slavicpolyglot.service.TranslationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity principal da versão Mobile (Android) do Tradutor,
 * com suporte a Tema Claro e Tema Escuro (Dark Mode) em tempo real.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class MainActivity extends Activity {

    private final TranslationService translationService = new TranslationService();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean darkMode = false;

    private LinearLayout screenRoot;
    private LinearLayout headerBar;
    private LinearLayout mainCard;
    private LinearLayout targetSection;
    private View dividerView;

    private TextView appTitle;
    private Button themeBtn;
    private Button swapBtn;
    private Button clearBtn;
    private Spinner sourceSpinner;
    private Spinner targetSpinner;
    private EditText inputEdit;
    private TextView outputText;
    private TextView detectedLabel;
    private TextView targetLangTitle;
    private TextView quickSamplesLabel;
    private final List<Button> actionChips = new ArrayList<>();

    private final Language[] sourceOptions = new Language[]{
            Language.AUTO, Language.PORTUGUESE, Language.ENGLISH, Language.SPANISH
    };
    private final Language[] targetOptions = Language.getTargetLanguages();

    private final Runnable debounceRunnable = this::performLiveTranslation;
    private boolean suppressWatcher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenRoot = new LinearLayout(this);
        screenRoot.setOrientation(LinearLayout.VERTICAL);

        screenRoot.addView(createGoogleHeaderBar());
        screenRoot.addView(createLanguagePillBar());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        body.setPadding(pad, dp(12), pad, pad);

        body.addView(createMainTranslationCard());
        body.addView(createQuickSamplesSection());

        scrollView.addView(body);
        screenRoot.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(screenRoot);
        applyTheme();
        performLiveTranslation();
    }

    private View createGoogleHeaderBar() {
        headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(dp(20), dp(14), dp(20), dp(14));

        appTitle = new TextView(this);
        appTitle.setText("🌐 Tradutor");
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);

        themeBtn = new Button(this);
        themeBtn.setAllCaps(false);
        themeBtn.setText("🌙 Escuro");
        themeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        themeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        themeBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        themeBtn.setOnClickListener(v -> {
            darkMode = !darkMode;
            applyTheme();
        });

        headerBar.addView(appTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerBar.addView(themeBtn);
        return headerBar;
    }

    private View createLanguagePillBar() {
        LinearLayout barWrapper = new LinearLayout(this);
        barWrapper.setOrientation(LinearLayout.HORIZONTAL);
        barWrapper.setGravity(Gravity.CENTER_VERTICAL);
        barWrapper.setPadding(dp(16), dp(10), dp(16), dp(4));

        ArrayAdapter<Language> srcAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, sourceOptions);
        sourceSpinner = new Spinner(this);
        sourceSpinner.setAdapter(srcAdapter);
        sourceSpinner.setSelection(0);

        swapBtn = new Button(this);
        swapBtn.setAllCaps(false);
        swapBtn.setText("⇄");
        swapBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        swapBtn.setOnClickListener(v -> swapLanguages());

        ArrayAdapter<Language> tgtAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, targetOptions);
        targetSpinner = new Spinner(this);
        targetSpinner.setAdapter(tgtAdapter);
        targetSpinner.setSelection(1);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Language tgt = (Language) targetSpinner.getSelectedItem();
                if (targetLangTitle != null && tgt != null) {
                    targetLangTitle.setText(tgt.getDisplayName().toUpperCase());
                }
                performLiveTranslation();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        sourceSpinner.setOnItemSelectedListener(listener);
        targetSpinner.setOnItemSelectedListener(listener);

        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        LinearLayout.LayoutParams swapLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        swapLp.setMargins(dp(8), 0, dp(8), 0);

        barWrapper.addView(sourceSpinner, spinLp);
        barWrapper.addView(swapBtn, swapLp);
        barWrapper.addView(targetSpinner, spinLp);
        return barWrapper;
    }

    private View createMainTranslationCard() {
        mainCard = new LinearLayout(this);
        mainCard.setOrientation(LinearLayout.VERTICAL);

        LinearLayout sourceSection = new LinearLayout(this);
        sourceSection.setOrientation(LinearLayout.VERTICAL);
        sourceSection.setPadding(dp(18), dp(14), dp(14), dp(12));

        LinearLayout srcTopRow = new LinearLayout(this);
        srcTopRow.setOrientation(LinearLayout.HORIZONTAL);
        srcTopRow.setGravity(Gravity.CENTER_VERTICAL);

        detectedLabel = new TextView(this);
        detectedLabel.setText("DIGITE O TEXTO");
        detectedLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        detectedLabel.setTypeface(Typeface.DEFAULT_BOLD);

        clearBtn = new Button(this);
        clearBtn.setAllCaps(false);
        clearBtn.setText("✕");
        clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        clearBtn.setBackgroundColor(Color.TRANSPARENT);
        clearBtn.setOnClickListener(v -> {
            inputEdit.setText("");
            outputText.setText("");
            detectedLabel.setText("DIGITE O TEXTO");
        });

        srcTopRow.addView(detectedLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        srcTopRow.addView(clearBtn, new LinearLayout.LayoutParams(dp(38), dp(36)));

        inputEdit = new EditText(this);
        inputEdit.setHint("Digite o texto para traduzir...");
        inputEdit.setText("Bom dia! Como você está hoje?");
        inputEdit.setMinLines(3);
        inputEdit.setMaxLines(6);
        inputEdit.setGravity(Gravity.TOP | Gravity.START);
        inputEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        inputEdit.setBackgroundColor(Color.TRANSPARENT);
        inputEdit.setPadding(0, dp(6), 0, dp(8));

        inputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressWatcher) return;
                mainHandler.removeCallbacks(debounceRunnable);
                if (s.toString().trim().isEmpty()) {
                    outputText.setText("");
                    return;
                }
                mainHandler.postDelayed(debounceRunnable, 350);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        sourceSection.addView(srcTopRow);
        sourceSection.addView(inputEdit);

        dividerView = new View(this);
        dividerView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        targetSection = new LinearLayout(this);
        targetSection.setOrientation(LinearLayout.VERTICAL);
        targetSection.setPadding(dp(18), dp(14), dp(18), dp(16));

        targetLangTitle = new TextView(this);
        targetLangTitle.setText("INGLÊS");
        targetLangTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        targetLangTitle.setTypeface(Typeface.DEFAULT_BOLD);

        outputText = new TextView(this);
        outputText.setText("Good morning! How are you today?");
        outputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        outputText.setMinLines(3);
        outputText.setPadding(0, dp(8), 0, dp(10));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.END);

        Button copyBtn = createActionChip("📋 Copiar tradução");
        copyBtn.setOnClickListener(v -> {
            String out = outputText.getText().toString().trim();
            if (!out.isEmpty()) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("translation", out));
                    Toast.makeText(this, "Tradução copiada!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        actionRow.addView(copyBtn);

        targetSection.addView(targetLangTitle);
        targetSection.addView(outputText);
        targetSection.addView(actionRow);

        mainCard.addView(sourceSection);
        mainCard.addView(dividerView);
        mainCard.addView(targetSection);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(14);
        mainCard.setLayoutParams(cardLp);
        return mainCard;
    }

    private View createQuickSamplesSection() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        quickSamplesLabel = new TextView(this);
        quickSamplesLabel.setText("Frases rápidas para testar:");
        quickSamplesLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        quickSamplesLabel.setPadding(dp(4), 0, 0, dp(8));
        box.addView(quickSamplesLabel);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (OfflineDictionary.PhraseRow phrase : OfflineDictionary.getAllPhrases()) {
            final String pt = phrase.getByLanguage(Language.PORTUGUESE);
            Button chip = createActionChip(pt);
            chip.setOnClickListener(v -> {
                sourceSpinner.setSelection(1);
                targetSpinner.setSelection(1);
                inputEdit.setText(pt);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            lp.rightMargin = dp(8);
            row.addView(chip, lp);
        }

        hsv.addView(row);
        box.addView(hsv);
        return box;
    }

    private void applyTheme() {
        String pageBg = darkMode ? "#0F172A" : "#F8F9FA";
        String cardBg = darkMode ? "#1E293B" : "#FFFFFF";
        String outBg = darkMode ? "#172030" : "#F1F3F4";
        String border = darkMode ? "#334155" : "#DADCE0";
        String priText = darkMode ? "#F1F5F9" : "#202124";
        String secText = darkMode ? "#94A3B8" : "#5F6368";
        String accent = darkMode ? "#8AB4F8" : "#1A73E8";

        screenRoot.setBackgroundColor(Color.parseColor(pageBg));
        headerBar.setBackgroundColor(Color.parseColor(cardBg));
        appTitle.setTextColor(Color.parseColor(priText));

        themeBtn.setText(darkMode ? "☀️ Claro" : "🌙 Escuro");
        themeBtn.setTextColor(Color.parseColor(priText));
        themeBtn.setBackground(roundedBox(pageBg, border, 18));

        sourceSpinner.setBackground(roundedBox(cardBg, border, 24));
        targetSpinner.setBackground(roundedBox(cardBg, border, 24));

        swapBtn.setTextColor(Color.parseColor(accent));
        swapBtn.setBackground(roundedBox(darkMode ? "#1E3A8A" : "#E8F0FE", border, 24));

        mainCard.setBackground(roundedBox(cardBg, border, 16));
        dividerView.setBackgroundColor(Color.parseColor(border));
        targetSection.setBackground(roundedBox(outBg, border, 12));

        detectedLabel.setTextColor(Color.parseColor(secText));
        clearBtn.setTextColor(Color.parseColor(secText));
        inputEdit.setTextColor(Color.parseColor(priText));
        inputEdit.setHintTextColor(Color.parseColor(secText));

        targetLangTitle.setTextColor(Color.parseColor(accent));
        outputText.setTextColor(Color.parseColor(priText));
        quickSamplesLabel.setTextColor(Color.parseColor(secText));

        for (Button chip : actionChips) {
            chip.setTextColor(Color.parseColor(accent));
            chip.setBackground(roundedBox(cardBg, border, 18));
        }
    }

    private void swapLanguages() {
        Language src = (Language) sourceSpinner.getSelectedItem();
        Language tgt = (Language) targetSpinner.getSelectedItem();

        if (src == Language.AUTO) {
            src = OfflineDictionary.guessLanguage(inputEdit.getText().toString());
        }

        for (int i = 0; i < sourceOptions.length; i++) {
            if (sourceOptions[i] == tgt) {
                sourceSpinner.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < targetOptions.length; i++) {
            if (targetOptions[i] == src) {
                targetSpinner.setSelection(i);
                break;
            }
        }

        String currentOut = outputText.getText().toString().trim();
        if (!currentOut.isEmpty() && !currentOut.equals("Traduzindo...")) {
            suppressWatcher = true;
            inputEdit.setText(currentOut);
            suppressWatcher = false;
            performLiveTranslation();
        }
    }

    private void performLiveTranslation() {
        final String text = inputEdit.getText().toString().trim();
        if (text.isEmpty()) {
            outputText.setText("");
            return;
        }

        final Language src = (Language) sourceSpinner.getSelectedItem();
        final Language tgt = (Language) targetSpinner.getSelectedItem();

        executor.execute(() -> {
            try {
                final TranslationEntry entry = translationService.translate(text, src, tgt);
                mainHandler.post(() -> {
                    outputText.setText(entry.getTranslatedText());
                    if (src == Language.AUTO) {
                        detectedLabel.setText("DETECTADO: " + entry.getSourceLang().getDisplayName().toUpperCase());
                    } else {
                        detectedLabel.setText(src.getDisplayName().toUpperCase());
                    }
                });
            } catch (final Exception e) {
                mainHandler.post(() -> outputText.setText("Sem conexão no momento."));
            }
        });
    }

    private Button createActionChip(String text) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        actionChips.add(b);
        return b;
    }

    private GradientDrawable roundedBox(String fillHex, String strokeHex, int radiusDp) {
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
