package com.greenenergy.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Builder
public class OptimalChargingWindowDto {
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Double averageCleanEnergyPercentage;
}
