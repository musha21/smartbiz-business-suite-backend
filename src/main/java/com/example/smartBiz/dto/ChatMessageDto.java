package com.example.smartBiz.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatMessageDto {
    private String message;
    private List<Map<String, String>> history;
}
