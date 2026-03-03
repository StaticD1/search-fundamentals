package com.kpfu.education.wordpreprocessor;

import com.kpfu.education.wordpreprocessor.service.HtmlProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class WordPreprocessorApplication implements CommandLineRunner {

    private final HtmlProcessingService service;

    public static void main(String[] args) {
        SpringApplication.run(WordPreprocessorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String inputDir = "src/main/resources/data/input";
        String outputDir = "src/main/resources/data/output";

        service.processFiles(inputDir, outputDir);
    }
}
