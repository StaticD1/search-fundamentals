package com.kpfu.education.wordpreprocessor.runner;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import com.kpfu.education.wordpreprocessor.service.indexer.IndexBuilder;
import com.kpfu.education.wordpreprocessor.service.search.BooleanEvaluator;
import com.kpfu.education.wordpreprocessor.service.search.BooleanQueryParser;
import com.kpfu.education.wordpreprocessor.service.tfidf.TfIdfCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchRunner implements CommandLineRunner {

    private final IndexBuilder indexBuilder;
    private final BooleanQueryParser queryParser;
    private final BooleanEvaluator evaluator;
    private final AppConfig appConfig;
    private final TfIdfCalculator tfIdfCalculator;

    @Value("${tfidf.enabled:false}")
    private boolean tfidfEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (tfidfEnabled) {
            log.info("Запуск расчета tf-idf...");
            tfIdfCalculator.calculate();
        }

        indexBuilder.buildIndex();

        System.out.println("Индекс загружен. Документов: " + indexBuilder.getAllDocuments().size());
        System.out.println("Введите запрос (например, (Клеопатра AND Цезарь) OR (Антоний AND Цицерон) OR Помпей)");
        System.out.println("Для выхода введите 'exit'.");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("\nЗапрос> ");
                String line = reader.readLine();
                if (line == null || line.equalsIgnoreCase("exit")) break;
                if (line.isBlank()) continue;

                try {
                    List<String> rpn = queryParser.toRPN(line);
                    Set<String> result = evaluator.evaluate(
                            rpn,
                            indexBuilder.getInvertedIndex(),
                            indexBuilder.getAllDocuments()
                    );

                    if (result.isEmpty()) {
                        System.out.println("Ничего не найдено.");
                    } else {
                        System.out.println("Найдено документов: " + result.size());
                        result.stream().sorted().forEach(System.out::println);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка: " + e.getMessage());
                }
            }
        }
    }
}