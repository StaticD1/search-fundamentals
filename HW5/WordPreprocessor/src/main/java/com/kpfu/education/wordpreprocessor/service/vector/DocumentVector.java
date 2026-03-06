package com.kpfu.education.wordpreprocessor.service.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
public class DocumentVector {
    private final String docId;
    private final Map<String, Double> weights;   // лемма -> tf-idf (ненормализованный)
    private double norm;                           // евклидова норма (вычисляется отдельно)

    public DocumentVector(String docId, Map<String, Double> weights) {
        this.docId = docId;
        this.weights = weights;
        this.norm = 0.0;
    }
}