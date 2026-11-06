package com.gabrila7.slavicpolyglot.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço utilitário para transliteração de texto em alfabeto cirílico (Russo)
 * para caracteres latinos (romanização fonética baseada no padrão GOST / BGN),
 * auxiliando na leitura e pronúncia de palavras russas.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class TransliterationService {

    private static final Map<Character, String> CYRILLIC_TO_LATIN = new HashMap<>();

    static {
        // Minúsculas
        CYRILLIC_TO_LATIN.put('а', "a");
        CYRILLIC_TO_LATIN.put('б', "b");
        CYRILLIC_TO_LATIN.put('в', "v");
        CYRILLIC_TO_LATIN.put('г', "g");
        CYRILLIC_TO_LATIN.put('д', "d");
        CYRILLIC_TO_LATIN.put('е', "ye");
        CYRILLIC_TO_LATIN.put('ё', "yo");
        CYRILLIC_TO_LATIN.put('ж', "zh");
        CYRILLIC_TO_LATIN.put('з', "z");
        CYRILLIC_TO_LATIN.put('и', "i");
        CYRILLIC_TO_LATIN.put('й', "y");
        CYRILLIC_TO_LATIN.put('к', "k");
        CYRILLIC_TO_LATIN.put('л', "l");
        CYRILLIC_TO_LATIN.put('м', "m");
        CYRILLIC_TO_LATIN.put('н', "n");
        CYRILLIC_TO_LATIN.put('о', "o");
        CYRILLIC_TO_LATIN.put('п', "p");
        CYRILLIC_TO_LATIN.put('р', "r");
        CYRILLIC_TO_LATIN.put('с', "s");
        CYRILLIC_TO_LATIN.put('т', "t");
        CYRILLIC_TO_LATIN.put('у', "u");
        CYRILLIC_TO_LATIN.put('ф', "f");
        CYRILLIC_TO_LATIN.put('х', "kh");
        CYRILLIC_TO_LATIN.put('ц', "ts");
        CYRILLIC_TO_LATIN.put('ч', "ch");
        CYRILLIC_TO_LATIN.put('ш', "sh");
        CYRILLIC_TO_LATIN.put('щ', "shch");
        CYRILLIC_TO_LATIN.put('ъ', "");
        CYRILLIC_TO_LATIN.put('ы', "y");
        CYRILLIC_TO_LATIN.put('ь', "'");
        CYRILLIC_TO_LATIN.put('э', "e");
        CYRILLIC_TO_LATIN.put('ю', "yu");
        CYRILLIC_TO_LATIN.put('я', "ya");

        // Maiúsculas
        CYRILLIC_TO_LATIN.put('А', "A");
        CYRILLIC_TO_LATIN.put('Б', "B");
        CYRILLIC_TO_LATIN.put('В', "V");
        CYRILLIC_TO_LATIN.put('Г', "G");
        CYRILLIC_TO_LATIN.put('Д', "D");
        CYRILLIC_TO_LATIN.put('Е', "Ye");
        CYRILLIC_TO_LATIN.put('Ё', "Yo");
        CYRILLIC_TO_LATIN.put('Ж', "Zh");
        CYRILLIC_TO_LATIN.put('З', "Z");
        CYRILLIC_TO_LATIN.put('И', "I");
        CYRILLIC_TO_LATIN.put('Й', "Y");
        CYRILLIC_TO_LATIN.put('К', "K");
        CYRILLIC_TO_LATIN.put('Л', "L");
        CYRILLIC_TO_LATIN.put('М', "M");
        CYRILLIC_TO_LATIN.put('Н', "N");
        CYRILLIC_TO_LATIN.put('О', "O");
        CYRILLIC_TO_LATIN.put('П', "P");
        CYRILLIC_TO_LATIN.put('Р', "R");
        CYRILLIC_TO_LATIN.put('С', "S");
        CYRILLIC_TO_LATIN.put('Т', "T");
        CYRILLIC_TO_LATIN.put('У', "U");
        CYRILLIC_TO_LATIN.put('Ф', "F");
        CYRILLIC_TO_LATIN.put('Х', "Kh");
        CYRILLIC_TO_LATIN.put('Ц', "Ts");
        CYRILLIC_TO_LATIN.put('Ч', "Ch");
        CYRILLIC_TO_LATIN.put('Ш', "Sh");
        CYRILLIC_TO_LATIN.put('Щ', "Shch");
        CYRILLIC_TO_LATIN.put('Ъ', "");
        CYRILLIC_TO_LATIN.put('Ы', "Y");
        CYRILLIC_TO_LATIN.put('Ь', "'");
        CYRILLIC_TO_LATIN.put('Э', "E");
        CYRILLIC_TO_LATIN.put('Ю', "Yu");
        CYRILLIC_TO_LATIN.put('Я', "Ya");
    }

    /**
     * Verifica se o texto contém pelo menos um caractere cirílico.
     */
    public static boolean containsCyrillic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converte texto em cirílico para sua representação fonética latina.
     */
    public static String toLatinPhonetic(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Ajuste contextual: 'е' após consoante soa mais próximo de 'e' do que 'ye'
            if ((c == 'е' || c == 'Е') && i > 0 && isRussianConsonant(text.charAt(i - 1))) {
                sb.append(c == 'е' ? "e" : "E");
            } else if (CYRILLIC_TO_LATIN.containsKey(c)) {
                sb.append(CYRILLIC_TO_LATIN.get(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isRussianConsonant(char c) {
        char lower = Character.toLowerCase(c);
        return "бвгджзйклмнпрстфхцчшщ".indexOf(lower) >= 0;
    }
}
