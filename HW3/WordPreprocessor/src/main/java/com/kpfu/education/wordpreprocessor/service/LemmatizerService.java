package com.kpfu.education.wordpreprocessor.service;

import com.github.demidko.aot.WordformMeaning;
import org.springframework.stereotype.Service;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.IOException;
import java.util.List;

@Service
public class LemmatizerService {

    private boolean isCyrillic(String word) {
        return word.codePoints().anyMatch(cp -> cp >= 0x0400 && cp <= 0x04FF);
    }

    public String normalize(String word) {
        String lowerWord = word.toLowerCase();
        if (isCyrillic(lowerWord)) {
            // Лемматизация для русского с помощью aot
            List<WordformMeaning> meanings = null;
            try {
                meanings = WordformMeaning.lookupForMeanings(lowerWord);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!meanings.isEmpty()) {
                // Для слова может быть несколько значений (омонимы), берем лемму первого
                return meanings.get(0).getLemma().toString();
            }
            return lowerWord; // если слово не найдено в словаре
        } else {
            // Стемминг для английского (Porter) — оставляем как есть
            EnglishStemmer stemmer = new EnglishStemmer();
            stemmer.setCurrent(lowerWord);
            if (stemmer.stem()) {
                return stemmer.getCurrent();
            }
            return lowerWord;
        }
    }
}