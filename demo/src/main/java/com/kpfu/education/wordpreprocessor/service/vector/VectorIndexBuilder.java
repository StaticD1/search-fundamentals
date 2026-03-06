package com.kpfu.education.wordpreprocessor.service.vector;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@Slf4j
@Getter
public class VectorIndexBuilder {

    private final AppConfig appConfig;
    private List<DocumentVector> documentVectors;
    private Map<String, Double> globalIdf;

    public VectorIndexBuilder(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @PostConstruct
    public void init() {
        try {
            loadVectors();
        } catch (IOException e) {
            log.error("Не удалось загрузить векторы документов", e);
            throw new RuntimeException("Ошибка инициализации векторного индекса", e);
        }
    }

    public void loadVectors() throws IOException {
        Path tfidfDir = Paths.get(appConfig.getOutputDir(), "tfidf_lemmas_per_doc");
        if (!Files.exists(tfidfDir)) {
            throw new IOException("Папка с tf-idf лемм не найдена: " + tfidfDir);
        }

        documentVectors = new ArrayList<>();
        globalIdf = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tfidfDir, "*_tfidf_lemmas.txt")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String docId = fileName.substring(0, fileName.length() - "_tfidf_lemmas.txt".length());

                Map<String, Double> weights = new HashMap<>();
                List<String> lines = Files.readAllLines(file);
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length < 3) continue;
                    String lemma = parts[0];
                    double idf = Double.parseDouble(parts[1].replace(',', '.'));
                    double tfidf = Double.parseDouble(parts[2].replace(',', '.'));
                    weights.put(lemma, tfidf);
                    globalIdf.putIfAbsent(lemma, idf);
                }

                if (!weights.isEmpty()) {
                    DocumentVector dv = new DocumentVector(docId, weights);
                    documentVectors.add(dv);
                }
            }
        }

        // Нормализуем все векторы
        for (DocumentVector dv : documentVectors) {
            double norm = 0.0;
            for (double w : dv.getWeights().values()) {
                norm += w * w;
            }
            norm = Math.sqrt(norm);
            dv.setNorm(norm);
        }

        log.info("Загружено {} векторов документов. Уникальных лемм с idf: {}", documentVectors.size(), globalIdf.size());
    }
}