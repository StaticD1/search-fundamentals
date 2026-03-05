package com.kpfu.education.wordpreprocessor.service;

import com.kpfu.education.wordpreprocessor.StopWords;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileProcessor {
    private final StopWords stopWords;
    private final LemmatizerService stemmerService;

    // Регулярное выражение для поиска последовательностей буквенных символов (Unicode)
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\p{L}+");

    public FileProcessor(StopWords stopWords, LemmatizerService stemmerService) {
        this.stopWords = stopWords;
        this.stemmerService = stemmerService;
    }

    /**
     * Обрабатывает один HTML-файл (в формате .txt).
     *
     * @param inputPath  путь к входному файлу
     * @param outputDir  директория для сохранения результатов
     * @throws IOException при ошибках чтения/записи
     */
    public void processFile(Path inputPath, Path outputDir) throws IOException {
        log.info("Обработка файла: {}", inputPath);

        // 1. Чтение содержимого
        String html = Files.readString(inputPath, StandardCharsets.UTF_8);

        // 2. Извлечение чистого текста из HTML с помощью Jsoup
        String text = Jsoup.parse(html).text();

        // 3. Токенизация
        List<String> rawTokens = tokenize(text);

        // 4. Фильтрация токенов (стоп-слова, мусор, числа)
        List<String> filteredTokens = filterTokens(rawTokens);

        // 5. Удаление дубликатов в пределах страницы
        Set<String> uniqueTokens = new TreeSet<>(filteredTokens); // TreeSet для сортировки

        // 6. Группировка токенов по леммам (стемам)
        Map<String, Set<String>> lemmaToTokens = groupByLemma(uniqueTokens);

        // 7. Сохранение результатов для текущей страницы
        String baseName = inputPath.getFileName().toString().replace(".txt", "");
        Path tokenFile = outputDir.resolve(baseName + "_tokens.txt");
        Path lemmaFile = outputDir.resolve(baseName + "_lemmas.txt");

        saveTokens(tokenFile, new ArrayList<>(uniqueTokens));
        saveLemmas(lemmaFile, lemmaToTokens);
    }

    /**
     * Разбивает текст на токены (слова), используя регулярное выражение для буквенных последовательностей.
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /**
     * Фильтрует токены: удаляет числа, слова с цифрами, стоп-слова и прочий мусор.
     */
    private List<String> filterTokens(List<String> tokens) {
        return tokens.stream()
                .filter(token -> token.length() >= 2)               // минимум 2 символа
                .filter(token -> !token.matches(".*\\d.*"))         // не содержит цифр
                .filter(token -> !isNumeric(token))                 // не является числом
                .filter(token -> !stopWords.isStopWord(token))      // не стоп-слово
                .filter(this::isNotNoise)                           // дополнительная очистка
                .map(String::toLowerCase)                            // приводим к нижнему регистру
                .collect(Collectors.toList());
    }

    /**
     * Проверяет, является ли строка числом (целым или с плавающей точкой).
     */
    private boolean isNumeric(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Проверка на «мусорные» токены:
     * - повтор одного символа (например, "aaaaa")
     * - чрезмерно длинные слова (> 30)
     * - слова, содержащие не ASCII символы? (Оставляем, так как русские буквы не ASCII)
     *   В оригинале Python отбрасывали не ASCII, но мы поддерживаем русский,
     *   поэтому оставляем все буквы Unicode.
     */
    private boolean isNotNoise(String token) {
        // Повтор одного символа (например, "zzzzz") считается мусором
        if (token.length() >= 5 && token.chars().distinct().count() == 1) {
            return false;
        }
        // Слишком длинные обрывки
        if (token.length() > 30) {
            return false;
        }
        return true;
    }

    /**
     * Группирует уникальные токены по леммам (с помощью стеммера).
     */
    private Map<String, Set<String>> groupByLemma(Set<String> tokens) {
        Map<String, Set<String>> map = new TreeMap<>(); // TreeMap для сортировки по лемме
        for (String token : tokens) {
            String lemma = stemmerService.normalize(token);
            // Защита: лемма не должна быть пустой или стоп-словом
            if (lemma.isEmpty() || stopWords.isStopWord(lemma)) {
                continue;
            }
            map.computeIfAbsent(lemma, k -> new TreeSet<>()).add(token);
        }
        return map;
    }

    /**
     * Сохраняет список токенов в файл (по одному на строку).
     */
    private void saveTokens(Path file, List<String> tokens) throws IOException {
        Files.write(file, tokens, StandardCharsets.UTF_8);
    }

    /**
     * Сохраняет группы токенов по леммам в формате:
     * <лемма> <токен1> <токен2> ...
     * (леммы и токены отсортированы)
     */
    private void saveLemmas(Path file, Map<String, Set<String>> lemmaToTokens) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : lemmaToTokens.entrySet()) {
            String lemma = entry.getKey();
            String tokensLine = String.join(" ", entry.getValue());
            lines.add(lemma + " " + tokensLine);
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}