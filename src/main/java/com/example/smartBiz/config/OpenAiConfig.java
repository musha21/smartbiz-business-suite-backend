package com.example.smartBiz.config;

import com.example.smartBiz.repository.SystemSettingRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api.key}")
    private String fallbackApiKey;

    @Autowired
    private SystemSettingRepo systemSettingRepo;

    public String getApiKey() {
        return systemSettingRepo.findByKey("openai.api.key")
                .map(s -> s.getValue())
                .filter(v -> v != null && !v.isBlank())
                .orElse(fallbackApiKey);
    }
}
