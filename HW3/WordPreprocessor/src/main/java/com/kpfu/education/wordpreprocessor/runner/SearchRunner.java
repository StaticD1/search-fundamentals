package com.kpfu.education.wordpreprocessor.runner;

import com.kpfu.education.wordpreprocessor.service.indexer.IndexBuilder;
import com.kpfu.education.wordpreprocessor.service.search.BooleanEvaluator;
import com.kpfu.education.wordpreprocessor.service.search.BooleanQueryParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void run(String... args) throws Exception {
        // Построить индекс
        indexBuilder.buildIndex();

        // Интерактивный ввод запросов
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("\nВведите запрос (или 'exit' для выхода): ");
                String line = reader.readLine();
                if (line == null || line.equalsIgnoreCase("exit")) {
                    break;
                }
                if (line.isBlank()) {
                    continue;
                }

                try {
                    // Парсинг в RPN
                    List<String> rpn = queryParser.toRPN(line);
                    log.debug("RPN: {}", rpn);

                    // Вычисление
                    Set<String> result = evaluator.evaluate(
                            rpn,
                            indexBuilder.getInvertedIndex(),
                            indexBuilder.getAllDocuments()
                    );

                    // Вывод результата
                    if (result.isEmpty()) {
                        System.out.println("Ничего не найдено.");
                    } else {
                        System.out.println("Найдено документов: " + result.size());
                        result.stream().sorted().forEach(System.out::println);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка обработки запроса: " + e.getMessage());
                }
            }
        }
    }
}