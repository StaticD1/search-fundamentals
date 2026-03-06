package com.kpfu.education.wordpreprocessor.service.vector;

import com.kpfu.education.wordpreprocessor.service.LemmatizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSearcher {

    private final LemmatizerService lemmatizerService;
    private final VectorIndexBuilder indexBuilder;

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\p{L}+");

    /**
     * Выполняет поиск по запросу, возвращает список документов с оценками, отсортированный по убыванию.
     *
     * @param query строка запроса
     * @param topN максимальное количество результатов
     * @return список Map.Entry (docId, score)
     */
    public List<Map.Entry<String, Double>> search(String query, int topN) {
        // 1. Токенизация запроса
        List<String> queryTokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(query);
        while (m.find()) {
            queryTokens.add(m.group().toLowerCase());
        }

        // 2. Подсчёт частот термов в запросе (tf_query)
        Map<String, Integer> termFreq = new HashMap<>();
        for (String token : queryTokens) {
            termFreq.put(token, termFreq.getOrDefault(token, 0) + 1);
        }

        // 3. Лемматизация каждого терма и построение взвешенного вектора запроса
        Map<String, Double> queryWeights = new HashMap<>(); // лемма -> вес (tf_query * idf)
        Map<String, Double> globalIdf = indexBuilder.getGlobalIdf();

        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String token = entry.getKey();
            int tf = entry.getValue();
            String lemma = lemmatizerService.normalize(token);
            Double idf = globalIdf.get(lemma);
            if (idf != null && idf > 0) {
                // Вес = tf_query * idf (можно модифицировать, например, логарифмический tf)
                queryWeights.put(lemma, tf * idf);
            }
        }

        if (queryWeights.isEmpty()) {
            return Collections.emptyList(); // нет совпадений
        }

        // 4. Нормализация вектора запроса
        double queryNorm = 0.0;
        for (double w : queryWeights.values()) {
            queryNorm += w * w;
        }
        queryNorm = Math.sqrt(queryNorm);
        // нормализуем веса (делим на норму)
        Map<String, Double> normalizedQuery = new HashMap<>();
        for (Map.Entry<String, Double> e : queryWeights.entrySet()) {
            normalizedQuery.put(e.getKey(), e.getValue() / queryNorm);
        }

        // 5. Вычисление косинусной близости со всеми документами
        List<DocumentVector> docs = indexBuilder.getDocumentVectors();
        List<Map.Entry<String, Double>> results = new ArrayList<>();

        for (DocumentVector doc : docs) {
            double dot = 0.0;
            // скалярное произведение нормализованных векторов: сумма по общим леммам
            for (Map.Entry<String, Double> qEntry : normalizedQuery.entrySet()) {
                String lemma = qEntry.getKey();
                Double docWeight = doc.getWeights().get(lemma);
                if (docWeight != null) {
                    dot += qEntry.getValue() * (docWeight / doc.getNorm());
                }
            }
            if (dot > 0) {
                results.add(new AbstractMap.SimpleEntry<>(doc.getDocId(), dot));
            }
        }

        // 6. Сортировка по убыванию
        results.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 7. Ограничение topN
        return results.stream().limit(topN).collect(Collectors.toList());
    }
}
