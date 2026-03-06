package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ImageGenerateRequestDto;
import com.example.smartBiz.dto.ImageGenerateResponseDto;
import com.example.smartBiz.service.AiImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AiImageController {

    private final AiImageService aiImageService;

    @PostMapping("/generate-image")
    public ResponseEntity<ImageGenerateResponseDto> generateImage(@RequestBody ImageGenerateRequestDto dto) {
        return ResponseEntity.ok(aiImageService.generateImage(dto));
    }
}
