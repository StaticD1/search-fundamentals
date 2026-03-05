package com.kpfu.education.wordpreprocessor;

import org.springframework.stereotype.Component;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class StopWords {
    // Объединённый список английских (из оригинального Python-кода) и русских стоп-слов
    private final Set<String> stopWords = Set.of(
            // Английские (NLTK + HTML-entity)
            "a","about","above","after","again","against","all","am","an","and","any","are","as","at",
            "be","because","been","before","being","below","between","both","but","by",
            "can","could","did","do","does","doing","down","during","each","few","for","from",
            "further","had","has","have","having","he","her","here","hers","herself","him","himself",
            "his","how","i","if","in","into","is","it","its","itself","just","me","more","most","my",
            "myself","no","nor","not","now","of","off","on","once","only","or","other","our","ours",
            "ourselves","out","over","own","same","she","should","so","some","such","than","that",
            "the","their","theirs","them","themselves","then","there","these","they","this","those",
            "through","to","too","under","until","up","very","was","we","were","what","when","where",
            "which","while","who","whom","why","will","with","would","you","your","yours","yourself",
            "yourselves",
            // Частые HTML-entity
            "nbsp","amp","lt","gt","quot","apos",

            // Русские стоп-слова (предлоги, союзы, частицы)
            "и","в","во","не","что","он","на","я","с","со","как","а","то","все","она","так","его",
            "но","да","ты","к","у","же","вы","за","бы","по","только","ее","мне","было","вот","от",
            "меня","еще","нет","о","из","ему","теперь","когда","даже","ну","вдруг","ли","если","уже",
            "или","быть","был","него","до","вас","нибудь","опять","уж","вам","ведь","там","потом",
            "себя","ничего","ей","может","они","тут","где","есть","надо","ней","для","мы","тебя","их",
            "чем","была","сам","чтоб","без","будто","чего","раз","тоже","себе","под","будет","ж","тогда",
            "кто","этот","того","потому","этого","какой","совсем","ним","здесь","этом","один","почти",
            "мой","тем","чтобы","нее","сейчас","были","куда","зачем","сказать","всех","никогда","сегодня",
            "можно","при","наконец","два","об","другой","хоть","после","над","больше","тот","через","эти",
            "нас","про","всего","них","какая","много","разве","три","эту","моя","впрочем","хорошо","свою",
            "этой","перед","иногда","лучше","чуть","том","нельзя","такой","им","более","всегда","конечно",
            "всю","между"
    );

    public boolean isStopWord(String word) {
        return stopWords.contains(word.toLowerCase());
    }
}