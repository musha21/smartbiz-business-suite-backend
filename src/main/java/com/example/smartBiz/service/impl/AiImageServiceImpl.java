package com.example.smartBiz.service.impl;

import com.example.smartBiz.config.OpenAiConfig;
import com.example.smartBiz.dto.ImageGenerateRequestDto;
import com.example.smartBiz.dto.ImageGenerateResponseDto;
import com.example.smartBiz.exception.AiException;
import com.example.smartBiz.service.AiImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiImageServiceImpl implements AiImageService {

    private final RestTemplate restTemplate;
    private final OpenAiConfig openAiConfig;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/images/generations";

    @Override
    public ImageGenerateResponseDto generateImage(ImageGenerateRequestDto dto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiConfig.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-image-1"); // Per user request
        body.put("prompt", dto.getPrompt());
        body.put("size", dto.getSize());


        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);
            Map responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map> dataList = (List<Map>) responseBody.get("data");
                if (!dataList.isEmpty()) {
                    String base64Image = (String) dataList.get(0).get("b64_json");
                    return new ImageGenerateResponseDto(base64Image);
                }
            }
            log.error("Empty or invalid response from OpenAI: {}", responseBody);
            throw new AiException("Empty response from OpenAI");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("OpenAI API error: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiException("OpenAI API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error during image generation", e);
            throw new AiException("Error generating image: " + e.getMessage(), e);
        }
    }
}
