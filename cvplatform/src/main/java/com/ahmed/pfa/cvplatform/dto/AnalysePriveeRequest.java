package com.ahmed.pfa.cvplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysePriveeRequest {
    private Long cvId;
    private Long offrePriveeId;
}