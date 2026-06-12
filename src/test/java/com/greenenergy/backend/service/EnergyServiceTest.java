package com.greenenergy.backend.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.greenenergy.backend.dto.external.CarbonIntensityResponse;
import com.greenenergy.backend.dto.response.OptimalChargingWindowDto;

@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {

    @Mock
    private CarbonApiClient carbonApiClient;

    @InjectMocks
    private EnergyService energyService;

    private CarbonIntensityResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new CarbonIntensityResponse();
    }

    @Test
    void getOptimalChargingWindow_ShouldFindBestWindow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        
        CarbonIntensityResponse.GenerationMix cleanMix = new CarbonIntensityResponse.GenerationMix();
        cleanMix.setFuel("wind");
        cleanMix.setPerc(80.0);

        CarbonIntensityResponse.GenerationMix dirtyMix = new CarbonIntensityResponse.GenerationMix();
        dirtyMix.setFuel("gas");
        dirtyMix.setPerc(90.0);

        CarbonIntensityResponse.CarbonIntensityData interval1 = new CarbonIntensityResponse.CarbonIntensityData();
        interval1.setFrom(now);
        interval1.setTo(now.plusMinutes(30));
        interval1.setGenerationmix(List.of(dirtyMix));

        CarbonIntensityResponse.CarbonIntensityData interval2 = new CarbonIntensityResponse.CarbonIntensityData();
        interval2.setFrom(now.plusMinutes(30));
        interval2.setTo(now.plusMinutes(60));
        interval2.setGenerationmix(List.of(cleanMix));

        CarbonIntensityResponse.CarbonIntensityData interval3 = new CarbonIntensityResponse.CarbonIntensityData();
        interval3.setFrom(now.plusMinutes(60));
        interval3.setTo(now.plusMinutes(90));
        interval3.setGenerationmix(List.of(cleanMix));

        mockResponse.setData(List.of(interval1, interval2, interval3));

        when(carbonApiClient.getGenerationMix(any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(mockResponse);

        OptimalChargingWindowDto result = energyService.getOptimalChargingWindow(1);

        assertNotNull(result);
        assertEquals(80.0, result.getAverageCleanEnergyPercentage());
        assertEquals(interval2.getFrom(), result.getStartTime());
        assertEquals(interval3.getTo(), result.getEndTime());
    }

    @Test
    void getOptimalChargingWindow_ShouldThrowExceptionForInvalidHours() {
        assertThrows(IllegalArgumentException.class, () -> energyService.getOptimalChargingWindow(0));
        assertThrows(IllegalArgumentException.class, () -> energyService.getOptimalChargingWindow(7));
    }
}
