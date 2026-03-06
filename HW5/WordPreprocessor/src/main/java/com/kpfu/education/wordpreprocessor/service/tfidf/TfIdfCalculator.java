package com.kpfu.education.wordpreprocessor.service.tfidf;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import com.kpfu.education.wordpreprocessor.service.LemmatizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class TfIdfCalculator {

    private final AppConfig appConfig;
    private final LemmatizerService lemmatizerService;

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\p{L}+");

    public void calculate() throws IOException {
        Path inputDir = Paths.get(appConfig.getInputDir());
        Path outputDir = Paths.get(appConfig.getOutputDir());

        if (!Files.exists(inputDir)) {
            throw new IOException("Входная папка не найдена: " + inputDir);
        }

        // Создаем выходные подпапки
        Path tokenOutDir = outputDir.resolve("tfidf_tokens_per_doc");
        Path lemmaOutDir = outputDir.resolve("tfidf_lemmas_per_doc");
        Files.createDirectories(tokenOutDir);
        Files.createDirectories(lemmaOutDir);

        // Список всех .txt файлов во входной папке
        List<Path> htmlFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.txt")) {
            for (Path file : stream) {
                htmlFiles.add(file);
            }
        }

        int N = htmlFiles.size();
        log.info("Найдено документов: {}", N);

        // Статистика по каждому документу
        List<DocumentStats> allStats = new ArrayList<>();
        // Инвертированные индексы для токенов и лемм (для df)
        Map<String, Set<String>> tokenDocMap = new HashMap<>();
        Map<String, Set<String>> lemmaDocMap = new HashMap<>();

        // Обрабатываем каждый документ
        for (Path file : htmlFiles) {
            String docId = file.getFileName().toString().replace(".txt", "");
            DocumentStats stats = processDocument(file, docId);
            allStats.add(stats);

            for (String token : stats.getTokenFreq().keySet()) {
                tokenDocMap.computeIfAbsent(token, k -> new HashSet<>()).add(docId);
            }
            for (String lemma : stats.getLemmaFreq().keySet()) {
                lemmaDocMap.computeIfAbsent(lemma, k -> new HashSet<>()).add(docId);
            }
        }

        log.info("Собрана статистика. Уникальных токенов: {}, уникальных лемм: {}",
                tokenDocMap.size(), lemmaDocMap.size());

        // Вычисление idf
        Map<String, Double> tokenIdf = new HashMap<>();
        Map<String, Double> lemmaIdf = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : tokenDocMap.entrySet()) {
            int df = entry.getValue().size();
            double idf = Math.log((double) N / df);
            tokenIdf.put(entry.getKey(), idf);
        }
        for (Map.Entry<String, Set<String>> entry : lemmaDocMap.entrySet()) {
            int df = entry.getValue().size();
            double idf = Math.log((double) N / df);
            lemmaIdf.put(entry.getKey(), idf);
        }

        // Генерация выходных файлов для каждого документа
        for (DocumentStats stats : allStats) {
            String docId = stats.getDocId();
            int totalTokens = stats.getTotalTokens();

            // Файл для токенов
            Path tokenFile = tokenOutDir.resolve(docId + "_tfidf_tokens.txt");
            List<String> tokenLines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : stats.getTokenFreq().entrySet()) {
                String token = entry.getKey();
                int freq = entry.getValue();
                double tf = (double) freq / totalTokens;
                double idf = tokenIdf.getOrDefault(token, 0.0);
                double tfidf = tf * idf;
                tokenLines.add(String.format("%s %.6f %.6f", token, idf, tfidf));
            }
            Files.write(tokenFile, tokenLines, StandardCharsets.UTF_8);

            // Файл для лемм
            Path lemmaFile = lemmaOutDir.resolve(docId + "_tfidf_lemmas.txt");
            List<String> lemmaLines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : stats.getLemmaFreq().entrySet()) {
                String lemma = entry.getKey();
                int freq = entry.getValue();
                double tf = (double) freq / totalTokens;
                double idf = lemmaIdf.getOrDefault(lemma, 0.0);
                double tfidf = tf * idf;
                lemmaLines.add(String.format("%s %.6f %.6f", lemma, idf, tfidf));
            }
            Files.write(lemmaFile, lemmaLines, StandardCharsets.UTF_8);
        }

        log.info("Расчет tf-idf завершен. Результаты сохранены в {} и {}", tokenOutDir, lemmaOutDir);
    }

    private DocumentStats processDocument(Path file, String docId) throws IOException {
        String html = Files.readString(file, StandardCharsets.UTF_8);
        String text = Jsoup.parse(html).text();

        // Токенизация с учетом всех буквенных последовательностей
        List<String> allTokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            allTokens.add(matcher.group().toLowerCase());
        }

        DocumentStats stats = new DocumentStats();
        stats.setDocId(docId);
        stats.setTotalTokens(allTokens.size());

        Map<String, Integer> tokenFreq = new HashMap<>();
        Map<String, Integer> lemmaFreq = new HashMap<>();

        for (String token : allTokens) {
            tokenFreq.put(token, tokenFreq.getOrDefault(token, 0) + 1);
            String lemma = lemmatizerService.normalize(token);
            lemmaFreq.put(lemma, lemmaFreq.getOrDefault(lemma, 0) + 1);
        }

        stats.setTokenFreq(tokenFreq);
        stats.setLemmaFreq(lemmaFreq);
        return stats;
    }
}
