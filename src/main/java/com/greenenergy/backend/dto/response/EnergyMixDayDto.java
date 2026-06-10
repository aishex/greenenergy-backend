package com.greenenergy.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class EnergyMixDayDto {
    private LocalDate date;
    private Map<String, Double> averageMix;
    private Double cleanEnergyPercentage;
}
