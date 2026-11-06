package com.gabrila7.slavicpolyglot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Armazena o resultado de uma tradução realizada, incluindo a transcrição
 * fonética (transliteração) quando o idioma envolve Russo.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class TranslationEntry {

    private final Language sourceLang;
    private final Language targetLang;
    private final String originalText;
    private final String translatedText;
    private final String transliteration;
    private final String engineUsed;
    private final String timestamp;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public TranslationEntry(Language sourceLang, Language targetLang, String originalText,
                            String translatedText, String transliteration, String engineUsed) {
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.transliteration = transliteration != null ? transliteration : "";
        this.engineUsed = engineUsed;
        this.timestamp = LocalDateTime.now().format(TIME_FMT);
    }

    public Language getSourceLang() {
        return sourceLang;
    }

    public Language getTargetLang() {
        return targetLang;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getTransliteration() {
        return transliteration;
    }

    public String getEngineUsed() {
        return engineUsed;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String formatSummaryLine() {
        String previewIn = originalText.length() > 35 ? originalText.substring(0, 32) + "..." : originalText;
        String previewOut = translatedText.length() > 35 ? translatedText.substring(0, 32) + "..." : translatedText;
        return String.format("[%s] %s -> %s: \"%s\" => \"%s\"",
                timestamp, sourceLang.getCode().toUpperCase(), targetLang.getCode().toUpperCase(),
                previewIn.replace("\n", " "), previewOut.replace("\n", " "));
    }
}
