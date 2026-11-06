package com.gabrila7.slavicpolyglot.service;

import com.gabrila7.slavicpolyglot.model.Language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dicionário e guia de conversação offline embutido com expressões comuns
 * e vocabulário essencial entre Inglês, Espanhol, Russo e Português.
 * Atua como fallback quando não há conexão com a internet e como base
 * rápida de consulta.
 *
 * @author Gabriel Vinícius (@Gabrila7)
 */
public class OfflineDictionary {

    public static class PhraseRow {
        private final String category;
        private final String english;
        private final String spanish;
        private final String russian;
        private final String portuguese;

        public PhraseRow(String category, String english, String spanish, String russian, String portuguese) {
            this.category = category;
            this.english = english;
            this.spanish = spanish;
            this.russian = russian;
            this.portuguese = portuguese;
        }

        public String getCategory() {
            return category;
        }

        public String getByLanguage(Language lang) {
            switch (lang) {
                case ENGLISH: return english;
                case SPANISH: return spanish;
                case RUSSIAN: return russian;
                case PORTUGUESE: return portuguese;
                default: return english;
            }
        }
    }

    private static final List<PhraseRow> PHRASES = new ArrayList<>();

    static {
        // Saudações e Cortesia
        add("Saudações", "Hello", "Hola", "Привет", "Olá");
        add("Saudações", "Good morning", "Buenos días", "Доброе утро", "Bom dia");
        add("Saudações", "Good afternoon", "Buenas tardes", "Добрый день", "Boa tarde");
        add("Saudações", "Good evening", "Buenas noches", "Добрый вечер", "Boa noite");
        add("Saudações", "Goodbye", "Adiós", "До свидания", "Até logo");
        add("Saudações", "See you later", "Hasta luego", "Увидимся позже", "Até mais tarde");
        add("Saudações", "How are you?", "¿Cómo estás?", "Как дела?", "Como vai você?");
        add("Saudações", "I am fine, thank you", "Estoy bien, gracias", "Всё хорошо, спасибо", "Estou bem, obrigado");
        add("Saudações", "What is your name?", "¿Cómo te llamas?", "Как тебя зовут?", "Qual é o seu nome?");
        add("Saudações", "My name is...", "Me llamo...", "Меня зовут...", "Meu nome é...");
        add("Saudações", "Nice to meet you", "Mucho gusto", "Очень приятно", "Muito prazer");

        // Essenciais do Dia a Dia
        add("Essenciais", "Thank you", "Gracias", "Спасибо", "Obrigado");
        add("Essenciais", "Thank you very much", "Muchas gracias", "Большое спасибо", "Muito obrigado");
        add("Essenciais", "Please", "Por favor", "Пожалуйста", "Por favor");
        add("Essenciais", "Excuse me", "Disculpe", "Извините", "Com licença / Desculpe");
        add("Essenciais", "I am sorry", "Lo siento", "Простите", "Sinto muito");
        add("Essenciais", "Yes", "Sí", "Да", "Sim");
        add("Essenciais", "No", "No", "Нет", "Não");
        add("Essenciais", "Maybe", "Tal vez", "Может быть", "Talvez");
        add("Essenciais", "Of course", "Por supuesto", "Конечно", "Com certeza");

        // Comunicação e Idiomas
        add("Idiomas", "Do you speak English?", "¿Hablas inglés?", "Вы говорите по-английски?", "Você fala inglês?");
        add("Idiomas", "Do you speak Spanish?", "¿Hablas español?", "Вы говорите по-испански?", "Você fala espanhol?");
        add("Idiomas", "Do you speak Russian?", "¿Hablas ruso?", "Вы говорите по-русски?", "Você fala russo?");
        add("Idiomas", "I do not understand", "No entiendo", "Я не понимаю", "Eu não entendo");
        add("Idiomas", "Please speak slower", "Por favor, hable más despacio", "Говорите медленнее, пожалуйста", "Por favor, fale mais devagar");
        add("Idiomas", "What does this mean?", "¿Qué significa esto?", "Что это значит?", "O que isto significa?");
        add("Idiomas", "How do you say this in Russian?", "¿Cómo se dice esto en ruso?", "Как это сказать по-русски?", "Como se diz isso em russo?");
        add("Idiomas", "I am learning Russian", "Estoy aprendiendo ruso", "Я изучаю русский язык", "Estou aprendendo russo");

        // Viagem e Direções
        add("Viagem", "Where is the train station?", "¿Dónde está la estación de tren?", "Где находится вокзал?", "Onde fica a estação de trem?");
        add("Viagem", "Where is the hotel?", "¿Dónde está el hotel?", "Где находится отель?", "Onde fica o hotel?");
        add("Viagem", "How much does it cost?", "¿Cuánto cuesta?", "Сколько это стоит?", "Quanto custa isto?");
        add("Viagem", "Can you help me?", "¿Puedes ayudarme?", "Вы можете мне помочь?", "Você pode me ajudar?");
        add("Viagem", "I need help", "Necesito ayuda", "Мне нужна помощь", "Preciso de ajuda");
        add("Viagem", "Where is the bathroom?", "¿Dónde está el baño?", "Где туалет?", "Onde fica o banheiro?");
        add("Viagem", "Water", "Agua", "Вода", "Água");
        add("Viagem", "Coffee", "Café", "Кофе", "Café");
        add("Viagem", "Restaurant", "Restaurante", "Ресторан", "Restaurante");
        add("Viagem", "Airport", "Aeropuerto", "Аэропорт", "Aeroporto");

        // Palavras soltas frequentes
        add("Vocabulário", "Friend", "Amigo", "Друг", "Amigo");
        add("Vocabulário", "House", "Casa", "Дом", "Casa");
        add("Vocabulário", "Work", "Trabajo", "Работа", "Trabalho");
        add("Vocabulário", "Time", "Tiempo", "Время", "Tempo");
        add("Vocabulário", "Today", "Hoy", "Сегодня", "Hoje");
        add("Vocabulário", "Tomorrow", "Mañana", "Завтра", "Amanhã");
        add("Vocabulário", "Yesterday", "Ayer", "Вчера", "Ontem");
        add("Vocabulário", "Book", "Libro", "Книга", "Livro");
        add("Vocabulário", "Computer", "Computadora", "Компьютер", "Computador");
        add("Vocabulário", "University", "Universidad", "Университет", "Universidade");
        add("Vocabulário", "Music", "Música", "Музыка", "Música");
        add("Vocabulário", "Peace", "Paz", "Мир", "Paz / Mundo");
        add("Vocabulário", "Love", "Amor", "Любовь", "Amor");
    }

    private static void add(String category, String en, String es, String ru, String pt) {
        PHRASES.add(new PhraseRow(category, en, es, ru, pt));
    }

    public static List<PhraseRow> getAllPhrases() {
        return PHRASES;
    }

    /**
     * Procura uma tradução exata ou por tokens simples no dicionário offline.
     * Retorna null caso não encontre correspondência direta.
     */
    public static String lookupOffline(String text, Language source, Language target) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalized = normalize(text);

        // 1. Busca por frase exata
        for (PhraseRow row : PHRASES) {
            if (normalize(row.getByLanguage(source)).equals(normalized)) {
                return row.getByLanguage(target);
            }
        }

        // 2. Busca palavra por palavra caso seja uma frase curta composta por itens do dicionário
        String[] words = text.trim().split("\\s+");
        if (words.length > 1 && words.length <= 6) {
            Map<String, String> wordMap = new LinkedHashMap<>();
            for (PhraseRow row : PHRASES) {
                wordMap.put(normalize(row.getByLanguage(source)), row.getByLanguage(target));
            }
            StringBuilder translatedWords = new StringBuilder();
            int matchedCount = 0;
            for (String w : words) {
                String cleanW = normalize(w);
                if (wordMap.containsKey(cleanW)) {
                    matchedCount++;
                    if (translatedWords.length() > 0) translatedWords.append(" ");
                    translatedWords.append(wordMap.get(cleanW));
                } else {
                    if (translatedWords.length() > 0) translatedWords.append(" ");
                    translatedWords.append(w);
                }
            }
            if (matchedCount > 0) {
                return translatedWords.toString();
            }
        }

        return null;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[?!.,;¿¡]", "");
    }
}
