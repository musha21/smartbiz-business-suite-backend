package com.example.smartBiz.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AiReportRequestDto {
    private String prompt;
    private LocalDate from;
    private LocalDate to;
}
