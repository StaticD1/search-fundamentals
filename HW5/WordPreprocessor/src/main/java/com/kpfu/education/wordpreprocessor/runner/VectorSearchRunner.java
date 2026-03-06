package com.kpfu.education.wordpreprocessor.runner;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import com.kpfu.education.wordpreprocessor.service.vector.VectorIndexBuilder;
import com.kpfu.education.wordpreprocessor.service.vector.VectorSearcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.mode", havingValue = "vector")
public class VectorSearchRunner implements CommandLineRunner {

    private final VectorIndexBuilder indexBuilder;
    private final VectorSearcher searcher;
    private final AppConfig appConfig;

    @Value("${search.topn}")
    private int topN;

    @Override
    public void run(String... args) throws Exception {
        // Загружаем векторы документов
        indexBuilder.loadVectors();

        System.out.println("Векторный индекс загружен. Документов: " + indexBuilder.getDocumentVectors().size());
        System.out.println("Введите запрос (или 'exit' для выхода):");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("\nЗапрос> ");
                String line = reader.readLine();
                if (line == null || line.equalsIgnoreCase("exit")) break;
                if (line.isBlank()) continue;

                long start = System.currentTimeMillis();
                List<Map.Entry<String, Double>> results = searcher.search(line, topN);
                long time = System.currentTimeMillis() - start;

                if (results.isEmpty()) {
                    System.out.println("Ничего не найдено.");
                } else {
                    System.out.printf("Найдено документов: %d (за %d мс)\n", results.size(), time);
                    for (Map.Entry<String, Double> res : results) {
                        System.out.printf("%s  (score: %.6f)\n", res.getKey(), res.getValue());
                    }
                }
            }
        }
    }
}
