package com.kpfu.education.wordpreprocessor.service;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.tartarus.snowball.ext.RussianStemmer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Service
public class HtmlProcessingService {

    // Регулярка: только буквы (кириллица и латиница)
    private static final Pattern CLEAN_PATTERN = Pattern.compile("^[а-яА-ЯёЁa-zA-Z]+$");

    // Стоп-слова из Lucene
    private final CharArraySet stopWords;

    public HtmlProcessingService() {
        this.stopWords = RussianAnalyzer.getDefaultStopSet();
    }

    public void processFiles(String inputDirectory, String outputDirectory) {
        Path inputPath = Paths.get(inputDirectory);
        Path outputPath = Paths.get(outputDirectory);

        try {
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            // Структуры для сбора данных со ВСЕХ страниц
            Set<String> globalUniqueTokens = new TreeSet<>(); // Сортированный сет токенов
            Map<String, Set<String>> globalLemmasMap = new TreeMap<>(); // Сортированная мапа лемм

            // Инициализируем стеммер один раз
            RussianStemmer stemmer = new RussianStemmer();

            // Обходим все файлы
            Files.walk(inputPath)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .forEach(file -> processSingleFile(file, stemmer, globalUniqueTokens, globalLemmasMap));

            // Записываем итоговые файлы
            System.out.println("Запись итоговых файлов...");
            writeTokensFile(outputPath, "tokens.txt", globalUniqueTokens);
            writeLemmasFile(outputPath, "lemmas.txt", globalLemmasMap);
            System.out.println("Обработка завершена.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processSingleFile(Path inputFile, RussianStemmer stemmer,
                                   Set<String> globalTokens, Map<String, Set<String>> globalLemmas) {
        try {
            String content = Files.readString(inputFile, StandardCharsets.UTF_8);
            // 1. Очистка HTML
            String text = Jsoup.parse(content).text();

            // 2. Токенизация
            String[] rawTokens = text.split("[^a-zA-Zа-яА-ЯёЁ]+");

            for (String token : rawTokens) {
                String lowerToken = token.toLowerCase().trim();

                if (lowerToken.isEmpty()) continue;

                // Фильтрация: стоп-слова и мусор
                if (!stopWords.contains(lowerToken) && CLEAN_PATTERN.matcher(lowerToken).matches()) {

                    // Добавляем токен в общий список
                    globalTokens.add(lowerToken);

                    // 3. Лемматизация (Стемминг)
                    stemmer.setCurrent(lowerToken);
                    stemmer.stem();
                    String lemma = stemmer.getCurrent();

                    // Добавляем связь лемма -> токен в общую карту
                    globalLemmas.computeIfAbsent(lemma, k -> new TreeSet<>()).add(lowerToken);
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка при обработке файла: " + inputFile + " -> " + e.getMessage());
        }
    }

    private void writeTokensFile(Path dir, String filename, Set<String> tokens) throws IOException {
        Path file = dir.resolve(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String token : tokens) {
                writer.write(token);
                writer.newLine();
            }
        }
    }

    private void writeLemmasFile(Path dir, String filename, Map<String, Set<String>> lemmasMap) throws IOException {
        Path file = dir.resolve(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Set<String>> entry : lemmasMap.entrySet()) {
                StringBuilder line = new StringBuilder(entry.getKey());
                for (String token : entry.getValue()) {
                    line.append(" ").append(token);
                }
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }
}