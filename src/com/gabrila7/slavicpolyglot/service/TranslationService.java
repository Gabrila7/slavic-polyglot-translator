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
 * Serviço responsável pela comunicação com APIs públicas de tradução
 * (com fallback automático em múltiplos provedores e dicionário offline)
 * e gerenciamento do histórico da sessão.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class TranslationService {

    private final List<TranslationEntry> history = new ArrayList<>();

    /**
     * Realiza a tradução do texto entre o idioma de origem e destino.
     * Tenta primeiro os endpoints online e recorre ao dicionário embutido caso
     * não haja conexão.
     */
    public TranslationEntry translate(String text, Language source, Language target) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Digite ou selecione um texto para traduzir.");
        }

        String trimmed = text.trim();
        if (source == target) {
            String translit = buildTransliterationIfApplicable(trimmed, trimmed, source, target);
            TranslationEntry entry = new TranslationEntry(source, target, trimmed, trimmed, translit, "Mesmo idioma");
            history.add(0, entry);
            return entry;
        }

        String translated = null;
        String engine = "Offline Dictionary";

        // 1. Tenta API rápida de tradução neural (Clients5 GTX)
        try {
            translated = queryGoogleClients5(trimmed, source.getCode(), target.getCode());
            if (translated != null && !translated.isEmpty()) {
                engine = "Neural Cloud API";
            }
        } catch (Exception ignored) {
            // Prossegue para o segundo provedor
        }

        // 2. Fallback para API pública MyMemory Translated
        if (translated == null || translated.isEmpty()) {
            try {
                translated = queryMyMemoryApi(trimmed, source.getCode(), target.getCode());
                if (translated != null && !translated.isEmpty()) {
                    engine = "MyMemory API";
                }
            } catch (Exception ignored) {
                // Prossegue para dicionário local
            }
        }

        // 3. Fallback para o dicionário offline interno
        if (translated == null || translated.isEmpty()) {
            translated = OfflineDictionary.lookupOffline(trimmed, source, target);
            if (translated != null) {
                engine = "Dicionário Offline";
            } else {
                throw new RuntimeException(
                        "Sem conexão com o servidor de tradução e frase não encontrada no dicionário offline.");
            }
        }

        String translit = buildTransliterationIfApplicable(trimmed, translated, source, target);
        TranslationEntry entry = new TranslationEntry(source, target, trimmed, translated, translit, engine);
        history.add(0, entry);
        return entry;
    }

    private String buildTransliterationIfApplicable(String original, String translated, Language source, Language target) {
        if (target == Language.RUSSIAN || TransliterationService.containsCyrillic(translated)) {
            return TransliterationService.toLatinPhonetic(translated);
        }
        if (source == Language.RUSSIAN || TransliterationService.containsCyrillic(original)) {
            return TransliterationService.toLatinPhonetic(original);
        }
        return "";
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
        if (idx < 0) {
            return null;
        }
        int start = idx + marker.length();
        return unescapeJsonStringUntilQuote(json, start);
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Accept-Charset", "UTF-8");

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("HTTP status " + status);
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

    /**
     * Extrai a primeira string JSON válida de um array JSON simples como ["Texto traduzido"].
     */
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

    public void clearHistory() {
        history.clear();
    }
}
