package com.gabrila7.slavicpolyglot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa o resultado de uma tradução realizada.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class TranslationEntry {

    private final Language sourceLang;
    private final Language targetLang;
    private final String originalText;
    private final String translatedText;
    private final String engineUsed;
    private final String timestamp;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public TranslationEntry(Language sourceLang, Language targetLang, String originalText,
                            String translatedText, String engineUsed) {
        this.sourceLang = sourceLang;
        this.targetLang = targetLang;
        this.originalText = originalText;
        this.translatedText = translatedText;
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

    public String getEngineUsed() {
        return engineUsed;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
