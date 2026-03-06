package com.example.smartBiz.service;

import com.example.smartBiz.dto.ImageGenerateRequestDto;
import com.example.smartBiz.dto.ImageGenerateResponseDto;

public interface AiImageService {
    ImageGenerateResponseDto generateImage(ImageGenerateRequestDto dto);
}
