package com.greenenergy.backend.dto.external;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CarbonIntensityResponse {
    
    private List<CarbonIntensityData> data;

    @Data
    public static class CarbonIntensityData {
        private ZonedDateTime from;
        private ZonedDateTime to;
        private List<GenerationMix> generationmix;
    }

    @Data
    public static class GenerationMix {
        private String fuel;
        private Double perc;
    }
}
