package com.kpfu.education.wordpreprocessor.service.search;

import com.kpfu.education.wordpreprocessor.service.LemmatizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BooleanEvaluator {

    private final LemmatizerService lemmatizerService;

    /**
     * Вычисляет результат запроса, представленного в RPN.
     *
     * @param rpn           список токенов в обратной польской нотации
     * @param index         инвертированный индекс (лемма -> множество документов)
     * @param allDocuments  множество всех документов
     * @return отсортированный список имён документов, удовлетворяющих запросу
     */
    public Set<String> evaluate(List<String> rpn,
                                Map<String, Set<String>> index,
                                Set<String> allDocuments) {
        Deque<Set<String>> stack = new ArrayDeque<>();

        for (String token : rpn) {
            if (isOperator(token)) {
                if (token.equalsIgnoreCase("NOT")) {
                    // Унарный оператор
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("NOT without operand");
                    }
                    Set<String> operand = stack.pop();
                    Set<String> result = new HashSet<>(allDocuments);
                    result.removeAll(operand);
                    stack.push(result);
                } else {
                    // Бинарные AND / OR
                    if (stack.size() < 2) {
                        throw new IllegalArgumentException("Insufficient operands for " + token);
                    }
                    Set<String> right = stack.pop();
                    Set<String> left = stack.pop();
                    Set<String> result;
                    if (token.equalsIgnoreCase("AND")) {
                        result = new HashSet<>(left);
                        result.retainAll(right);
                    } else if (token.equalsIgnoreCase("OR")) {
                        result = new HashSet<>(left);
                        result.addAll(right);
                    } else {
                        throw new IllegalArgumentException("Unknown operator: " + token);
                    }
                    stack.push(result);
                }
            } else {
                // Операнд — терм (слово)
                String term = lemmatizerService.normalize(token);
                Set<String> docs = index.getOrDefault(term, Collections.emptySet());
                stack.push(new HashSet<>(docs));
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression: leftover stack size " + stack.size());
        }
        return stack.pop();
    }

    private boolean isOperator(String token) {
        return token.equalsIgnoreCase("AND") ||
                token.equalsIgnoreCase("OR") ||
                token.equalsIgnoreCase("NOT");
    }
}