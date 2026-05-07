package com.example.smartBiz.controller;

import com.example.smartBiz.dto.ImageGenerateRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.smartBiz.dto.ImageGenerateResponseDto;
import com.example.smartBiz.service.AiImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/ai")
@RequiredArgsConstructor
@CrossOrigin
@Tag(name = "AI Image", description = "AI image generation")
public class AiImageController {

    private final AiImageService aiImageService;

    @Operation(summary = "Generate AI image", description = "Generates an image using AI based on the provided prompt. Returns a URL to the generated image.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image generated"),
            @ApiResponse(responseCode = "502", description = "AI service error")
    })
    @PostMapping("/generate-image")
    public ResponseEntity<ImageGenerateResponseDto> generateImage(@RequestBody ImageGenerateRequestDto dto) {
        return ResponseEntity.ok(aiImageService.generateImage(dto));
    }
}
