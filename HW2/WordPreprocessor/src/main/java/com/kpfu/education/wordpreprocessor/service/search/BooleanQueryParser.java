package com.kpfu.education.wordpreprocessor.service.search;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class BooleanQueryParser {

    private static final Set<String> OPERATORS = Set.of("AND", "OR", "NOT");
    private static final Map<String, Integer> PRECEDENCE = Map.of(
            "NOT", 3,
            "AND", 2,
            "OR", 1
    );

    /**
     * Преобразует инфиксный запрос в обратную польскую нотацию (RPN).
     * Поддерживает скобки, AND, OR, NOT (унарный).
     */
    public List<String> toRPN(String query) {
        List<String> output = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();

        // Токенизация: разбиваем на слова и операторы, учитывая скобки
        List<String> tokens = tokenize(query);

        for (String token : tokens) {
            if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty() || !stack.peek().equals("(")) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
                stack.pop(); // убираем '('
            } else if (isOperator(token)) {
                // Оператор
                while (!stack.isEmpty() && isOperator(stack.peek())
                        && (PRECEDENCE.get(token) <= PRECEDENCE.get(stack.peek()))) {
                    output.add(stack.pop());
                }
                stack.push(token);
            } else {
                // Операнд (терм)
                output.add(token);
            }
        }

        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (op.equals("(")) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(op);
        }

        return output;
    }

    /**
     * Разбивает строку запроса на токены: операторы, скобки, слова.
     */
    private List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inWord = false;

        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (Character.isLetter(c) || c == '-') { // разрешаем дефис в словах
                current.append(c);
                inWord = true;
            } else {
                if (inWord) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    inWord = false;
                }
                if (c == '(' || c == ')') {
                    tokens.add(String.valueOf(c));
                } else if (!Character.isWhitespace(c)) {
                    // Игнорируем другие символы (можно считать разделителями)
                }
            }
        }
        if (inWord) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private boolean isOperator(String token) {
        return OPERATORS.contains(token.toUpperCase());
    }
}