package com.kpfu.education.wordpreprocessor;

import com.kpfu.education.wordpreprocessor.service.LemmatizerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class WordPreprocessorApplication {

    private final LemmatizerService service;

    public static void main(String[] args) {
        SpringApplication.run(WordPreprocessorApplication.class, args);
    }
}
