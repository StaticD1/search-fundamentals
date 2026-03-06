package com.kpfu.education.wordpreprocessor.controller;

import com.kpfu.education.wordpreprocessor.dto.SearchResultDto;
import com.kpfu.education.wordpreprocessor.service.vector.VectorSearcher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class VectorSearchController {

    private final VectorSearcher vectorSearcher;

    @GetMapping
    public List<SearchResultDto> search(@RequestParam("q") String query) {
        // Запрашиваем топ-10 результатов
        return vectorSearcher.search(query, 10);
    }
}