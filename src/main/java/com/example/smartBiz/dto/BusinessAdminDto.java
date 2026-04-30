package com.example.smartBiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessAdminDto {
    private Long id;
    private String name;
    private Boolean active;
    private String email;
    private String contactNumber;

    


}
