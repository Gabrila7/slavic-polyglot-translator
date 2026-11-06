package com.gabrila7.slavicpolyglot.model;

/**
 * Layouts para o teclado virtual cirílico (Russo).
 * Suporta o padrão oficial JCUKEN (ЙЦУКЕН) e o modo Fonético (QWERTY -> Cirílico)
 * para facilitar a digitação em teclados ocidentais.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public enum KeyboardLayout {
    JCUKEN("Padrão Russo (ЙЦУКЕН)"),
    PHONETIC("Fonético (QWERTY -> ЯШЕРТЫ)");

    private final String label;

    KeyboardLayout(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Retorna as fileiras de teclas em minúsculas para o layout selecionado.
     */
    public String[][] getRows(boolean uppercase) {
        String[][] base;
        if (this == JCUKEN) {
            base = new String[][]{
                {"ё", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="},
                {"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"},
                {"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"},
                {"я", "ч", "с", "м", "и", "т", "ь", "б", "ю", ".", "?", "!"}
            };
        } else {
            // Layout fonético onde A=А, B=Б, V=В, G=Г, D=Д, E=Е, O=О, P=П, R=Р, S=С, T=Т, etc.
            base = new String[][]{
                {"ё", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "ч", "щ"},
                {"я", "ш", "е", "р", "т", "ы", "у", "и", "о", "п", "ю", "ж"},
                {"а", "с", "д", "ф", "г", "х", "й", "к", "л", "э", "ъ"},
                {"з", "ь", "ц", "в", "б", "н", "м", ",", ".", "?", "!"}
            };
        }

        if (!uppercase) {
            return base;
        }

        String[][] upper = new String[base.length][];
        for (int r = 0; r < base.length; r++) {
            upper[r] = new String[base[r].length];
            for (int c = 0; c < base[r].length; c++) {
                upper[r][c] = base[r][c].toUpperCase();
            }
        }
        return upper;
    }

    @Override
    public String toString() {
        return label;
    }
}
