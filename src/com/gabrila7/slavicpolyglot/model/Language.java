package com.gabrila7.slavicpolyglot.model;

/**
 * Idiomas suportados pelo tradutor (Português, Inglês e Espanhol),
 * além do modo de detecção automática de idioma de entrada.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public enum Language {
    AUTO("auto", "Detectar idioma"),
    PORTUGUESE("pt", "Português"),
    ENGLISH("en", "Inglês"),
    SPANISH("es", "Espanhol");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Language[] getTargetLanguages() {
        return new Language[]{PORTUGUESE, ENGLISH, SPANISH};
    }

    @Override
    public String toString() {
        return displayName;
    }
}
