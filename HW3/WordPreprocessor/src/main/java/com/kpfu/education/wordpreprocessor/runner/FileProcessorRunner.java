package com.kpfu.education.wordpreprocessor.runner;

import com.kpfu.education.wordpreprocessor.config.AppConfig;
import com.kpfu.education.wordpreprocessor.service.FileProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileProcessorRunner implements CommandLineRunner {

    private final FileProcessor fileProcessor;
    private final AppConfig appConfig;

    @Override
    public void run(String... args) throws Exception {
        Path inputDir = Paths.get(appConfig.getInputDir());
        Path outputDir = Paths.get(appConfig.getOutputDir());

        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            log.error("Входная директория не существует или не является папкой: {}", inputDir);
            return;
        }

        // Создаём выходную директорию, если её нет
        Files.createDirectories(outputDir);

        // Обрабатываем все файлы с расширением .txt
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.txt")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    try {
                        fileProcessor.processFile(file, outputDir);
                    } catch (IOException e) {
                        log.error("Ошибка обработки файла {}: {}", file, e.getMessage());
                    }
                }
            }
        }

        log.info("Обработка завершена. Результаты сохранены в {}", outputDir);
    }
}
