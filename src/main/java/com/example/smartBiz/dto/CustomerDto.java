package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CustomerDto {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;

    private Boolean archived;

}
