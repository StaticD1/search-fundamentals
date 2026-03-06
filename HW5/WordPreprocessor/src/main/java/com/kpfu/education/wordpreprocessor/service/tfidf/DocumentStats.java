package com.kpfu.education.wordpreprocessor.service.tfidf;

import lombok.Data;
import java.util.Map;

@Data
public class DocumentStats {
    private String docId;
    private int totalTokens;
    private Map<String, Integer> tokenFreq;
    private Map<String, Integer> lemmaFreq;
}
