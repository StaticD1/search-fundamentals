package com.kpfu.education.wordpreprocessor.service.vector;

import com.kpfu.education.wordpreprocessor.dto.SearchResultDto;
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

    public List<SearchResultDto> search(String query, int topN) {
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

        // 3. Лемматизация и построение вектора запроса
        Map<String, Double> queryWeights = new HashMap<>();
        Map<String, Double> globalIdf = indexBuilder.getGlobalIdf();

        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String token = entry.getKey();
            int tf = entry.getValue();
            String lemma = lemmatizerService.normalize(token);
            Double idf = globalIdf.get(lemma);
            if (idf != null && idf > 0) {
                queryWeights.put(lemma, tf * idf);
            }
        }

        if (queryWeights.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Нормализация вектора запроса
        double queryNorm = 0.0;
        for (double w : queryWeights.values()) {
            queryNorm += w * w;
        }
        queryNorm = Math.sqrt(queryNorm);
        Map<String, Double> normalizedQuery = new HashMap<>();
        for (Map.Entry<String, Double> e : queryWeights.entrySet()) {
            normalizedQuery.put(e.getKey(), e.getValue() / queryNorm);
        }

        // 5. Вычисление косинусной близости
        List<DocumentVector> docs = indexBuilder.getDocumentVectors();
        List<SearchResultDto> results = new ArrayList<>();

        for (DocumentVector doc : docs) {
            double dot = 0.0;
            for (Map.Entry<String, Double> qEntry : normalizedQuery.entrySet()) {
                String lemma = qEntry.getKey();
                Double docWeight = doc.getWeights().get(lemma);
                if (docWeight != null) {
                    dot += qEntry.getValue() * (docWeight / doc.getNorm());
                }
            }
            if (dot > 0) {
                results.add(new SearchResultDto(doc.getDocId(), dot));
            }
        }

        // 6. Сортировка и ограничение topN
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.stream().limit(topN).collect(Collectors.toList());
    }
}