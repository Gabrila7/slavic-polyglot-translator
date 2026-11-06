package com.gabrila7.slavicpolyglot.service;

import com.gabrila7.slavicpolyglot.model.Language;
import com.gabrila7.slavicpolyglot.model.TranslationEntry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de tradução automática entre Português, Inglês e Espanhol.
 * Funciona de forma contínua (tempo real conforme o usuário digita),
 * com detecção automática de idioma e fallback offline.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class TranslationService {

    private final List<TranslationEntry> history = new ArrayList<>();

    public TranslationEntry translate(String text, Language source, Language target) {
        if (text == null || text.trim().isEmpty()) {
            return new TranslationEntry(source, target, "", "", "Pronto");
        }

        String trimmed = text.trim();
        Language effectiveSource = source == Language.AUTO ? OfflineDictionary.guessLanguage(trimmed) : source;

        if (effectiveSource == target) {
            return new TranslationEntry(effectiveSource, target, trimmed, trimmed, "Mesmo idioma");
        }

        String translated = null;
        String engine = "Offline";

        // 1. Provedor principal (Google Translate API endpoint)
        try {
            String slCode = source == Language.AUTO ? "auto" : source.getCode();
            translated = queryGoogleClients5(trimmed, slCode, target.getCode());
            if (translated != null && !translated.isEmpty()) {
                engine = "Google Neural API";
            }
        } catch (Exception ignored) {
        }

        // 2. Provedor secundário (MyMemory API)
        if (translated == null || translated.isEmpty()) {
            try {
                translated = queryMyMemoryApi(trimmed, effectiveSource.getCode(), target.getCode());
                if (translated != null && !translated.isEmpty()) {
                    engine = "MyMemory API";
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Dicionário local caso sem internet
        if (translated == null || translated.isEmpty()) {
            translated = OfflineDictionary.lookupOffline(trimmed, effectiveSource, target);
            if (translated != null) {
                engine = "Dicionário Offline";
            } else {
                throw new RuntimeException("Sem conexão com a internet no momento.");
            }
        }

        TranslationEntry entry = new TranslationEntry(effectiveSource, target, trimmed, translated, engine);
        history.add(0, entry);
        return entry;
    }

    private String queryGoogleClients5(String text, String sl, String tl) throws Exception {
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        String endpoint = String.format(
                "https://clients5.google.com/translate_a/t?client=dict-chrome-ex&sl=%s&tl=%s&q=%s",
                sl, tl, encoded);

        String json = httpGet(endpoint);
        return extractFirstJsonString(json);
    }

    private String queryMyMemoryApi(String text, String sl, String tl) throws Exception {
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        String endpoint = String.format(
                "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s",
                encoded, sl, tl);

        String json = httpGet(endpoint);
        String marker = "\"translatedText\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        return unescapeJsonStringUntilQuote(json, idx + marker.length());
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Accept-Charset", "UTF-8");

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("HTTP " + status);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private String extractFirstJsonString(String json) {
        if (json == null) return null;
        int firstQuote = json.indexOf('"');
        if (firstQuote < 0) return null;
        return unescapeJsonStringUntilQuote(json, firstQuote + 1);
    }

    private String unescapeJsonStringUntilQuote(String json, int startIndex) {
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(c); break;
                }
                escaping = false;
            } else {
                if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString().trim();
    }

    public List<TranslationEntry> getHistory() {
        return history;
    }
}
