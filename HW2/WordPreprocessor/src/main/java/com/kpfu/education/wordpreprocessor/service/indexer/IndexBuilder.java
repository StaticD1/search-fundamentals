package com.kpfu.education.wordpreprocessor.service.indexer;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

        // Ищем все файлы, оканчивающиеся на "_lemmas.txt" (файлы лемм для документов)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir, "*_lemmas.txt")) {
            for (Path file : stream) {
                // Имя файла без суффикса "_lemmas.txt" будет идентификатором документа
                String fileName = file.getFileName().toString();
                String docId = fileName.substring(0, fileName.length() - "_lemmas.txt".length());
                allDocuments.add(docId);

                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(" ");
                    if (parts.length == 0) continue;
                    String lemma = parts[0]; // первое слово — лемма
                    invertedIndex.computeIfAbsent(lemma, k -> new HashSet<>()).add(docId);
                }
            }
        }

        log.info("Индекс построен. Всего лемм: {}, документов: {}", invertedIndex.size(), allDocuments.size());
    }
}
