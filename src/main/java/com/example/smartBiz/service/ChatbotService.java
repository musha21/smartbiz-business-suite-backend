package com.example.smartBiz.service;

import com.example.smartBiz.dto.ChatResponseDto;

import java.util.List;
import java.util.Map;

public interface ChatbotService {
    ChatResponseDto chat(Long businessId, String message, List<Map<String, String>> history);
}
