package com.kpfu.education.wordpreprocessor.service.indexer;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
@Slf4j
@Getter
public class IndexBuilder {

    private final AppConfig appConfig;
    private Map<String, Set<String>> invertedIndex;
    private Set<String> allDocuments;

    public IndexBuilder(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void buildIndex() throws IOException {
        Path outputDir = Paths.get(appConfig.getOutputDir());
        if (!Files.exists(outputDir)) {
            throw new IOException("Папка не найдена: " + outputDir);
        }

        invertedIndex = new HashMap<>();
        allDocuments = new HashSet<>();

        // Обрабатываем все файлы, оканчивающиеся на "_lemmas.txt"
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "*_lemmas.txt")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String docId = fileName.substring(0, fileName.length() - "_lemmas.txt".length());
                allDocuments.add(docId);

                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(" ");
                    if (parts.length == 0) continue;
                    String lemma = parts[0];
                    invertedIndex.computeIfAbsent(lemma, k -> new HashSet<>()).add(docId);
                }
            }
        }

        log.info("Индекс построен. Всего лемм: {}, документов: {}", invertedIndex.size(), allDocuments.size());

        // Сохраняем индекс в файл
        saveIndex(outputDir.resolve("inverted_index.txt"));
    }

    /**
     * Сохраняет инвертированный индекс в текстовый файл.
     * Формат строки: <лемма> <документ1> <документ2> ...
     */
    private void saveIndex(Path indexPath) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : invertedIndex.entrySet()) {
            String lemma = entry.getKey();
            String docsLine = String.join(" ", entry.getValue());
            lines.add(lemma + " " + docsLine);
        }
        Files.write(indexPath, lines, StandardCharsets.UTF_8);
        log.info("Индекс сохранён в файл: {}", indexPath);
    }
}
