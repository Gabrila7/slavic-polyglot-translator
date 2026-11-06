package com.gabrila7.slavicpolyglot.model;

/**
 * Representa os idiomas suportados pela aplicação de tradução.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public enum Language {
    ENGLISH("en", "Inglês (English)", "🇬🇧 EN"),
    SPANISH("es", "Espanhol (Español)", "🇪🇸 ES"),
    RUSSIAN("ru", "Russo (Русский)", "🇷🇺 RU"),
    PORTUGUESE("pt", "Português (Brasil)", "🇧🇷 PT");

    private final String code;
    private final String displayName;
    private final String badge;

    Language(String code, String displayName, String badge) {
        this.code = code;
        this.displayName = displayName;
        this.badge = badge;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadge() {
        return badge;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
