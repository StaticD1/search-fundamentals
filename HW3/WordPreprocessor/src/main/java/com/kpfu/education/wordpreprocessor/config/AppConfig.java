package com.kpfu.education.wordpreprocessor.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfig {
    @Value("${app.input-dir}")
    private String inputDir;

    @Value("${app.output-dir}")
    private String outputDir;

}
