package com.kpfu.education.wordpreprocessor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResultDto {
    private String docId;
    private double score;
}
