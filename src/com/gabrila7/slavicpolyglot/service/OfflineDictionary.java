package com.gabrila7.slavicpolyglot.service;

import com.gabrila7.slavicpolyglot.model.Language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Base local de expressões e vocabulário entre Português, Inglês e Espanhol.
 * Garante funcionamento offline para frases do cotidiano.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class OfflineDictionary {

    public static class PhraseRow {
        private final String category;
        private final String portuguese;
        private final String english;
        private final String spanish;

        public PhraseRow(String category, String portuguese, String english, String spanish) {
            this.category = category;
            this.portuguese = portuguese;
            this.english = english;
            this.spanish = spanish;
        }

        public String getCategory() {
            return category;
        }

        public String getByLanguage(Language lang) {
            switch (lang) {
                case PORTUGUESE: return portuguese;
                case ENGLISH: return english;
                case SPANISH: return spanish;
                default: return english;
            }
        }
    }

    private static final List<PhraseRow> PHRASES = new ArrayList<>();

    static {
        add("Saudações", "Olá", "Hello", "Hola");
        add("Saudações", "Bom dia", "Good morning", "Buenos días");
        add("Saudações", "Boa tarde", "Good afternoon", "Buenas tardes");
        add("Saudações", "Boa noite", "Good evening", "Buenas noches");
        add("Saudações", "Como vai você?", "How are you?", "¿Cómo estás?");
        add("Saudações", "Estou bem, obrigado", "I am fine, thank you", "Estoy bien, gracias");
        add("Saudações", "Muito prazer em conhecer você", "Nice to meet you", "Mucho gusto en conocerte");
        add("Saudações", "Até logo", "See you later", "Hasta luego");

        add("Cotidiano", "Por favor", "Please", "Por favor");
        add("Cotidiano", "Muito obrigado", "Thank you very much", "Muchas gracias");
        add("Cotidiano", "De nada", "You are welcome", "De nada");
        add("Cotidiano", "Com licença", "Excuse me", "Con permiso / Disculpe");
        add("Cotidiano", "Você pode me ajudar?", "Can you help me?", "¿Puedes ayudarme?");
        add("Cotidiano", "Eu não entendi", "I did not understand", "No entendí");
        add("Cotidiano", "Você fala inglês?", "Do you speak English?", "¿Hablas inglés?");
        add("Cotidiano", "Você fala espanhol?", "Do you speak Spanish?", "¿Hablas español?");

        add("Viagem", "Onde fica o hotel?", "Where is the hotel?", "¿Dónde está el hotel?");
        add("Viagem", "Quanto custa isso?", "How much does this cost?", "¿Cuánto cuesta esto?");
        add("Viagem", "Onde fica o aeroporto?", "Where is the airport?", "¿Dónde está el aeropuerto?");
        add("Viagem", "A conta, por favor", "The check, please", "La cuenta, por favor");
    }

    private static void add(String category, String pt, String en, String es) {
        PHRASES.add(new PhraseRow(category, pt, en, es));
    }

    public static List<PhraseRow> getAllPhrases() {
        return PHRASES;
    }

    public static String lookupOffline(String text, Language source, Language target) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalized = normalize(text);

        Language effectiveSource = source == Language.AUTO ? guessLanguage(text) : source;

        for (PhraseRow row : PHRASES) {
            if (normalize(row.getByLanguage(effectiveSource)).equals(normalized)) {
                return row.getByLanguage(target);
            }
        }

        String[] words = text.trim().split("\\s+");
        if (words.length > 1 && words.length <= 6) {
            Map<String, String> wordMap = new LinkedHashMap<>();
            for (PhraseRow row : PHRASES) {
                wordMap.put(normalize(row.getByLanguage(effectiveSource)), row.getByLanguage(target));
            }
            StringBuilder sb = new StringBuilder();
            int matched = 0;
            for (String w : words) {
                String clean = normalize(w);
                if (wordMap.containsKey(clean)) {
                    matched++;
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(wordMap.get(clean));
                } else {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(w);
                }
            }
            if (matched > 0) {
                return sb.toString();
            }
        }

        return null;
    }

    public static Language guessLanguage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("¿") || lower.contains("¡") || lower.contains("ñ")
                || lower.contains("hola") || lower.contains("gracias") || lower.contains("buenos")) {
            return Language.SPANISH;
        }
        if (lower.contains("ã") || lower.contains("ç") || lower.contains("você")
                || lower.contains("obrigado") || lower.contains("olá") || lower.contains("bom dia")) {
            return Language.PORTUGUESE;
        }
        return Language.ENGLISH;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("[?!.,;¿¡]", "");
    }
}
