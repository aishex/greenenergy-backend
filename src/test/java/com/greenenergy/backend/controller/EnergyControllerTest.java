package com.greenenergy.backend.controller;

import com.greenenergy.backend.dto.response.OptimalChargingWindowDto;
import com.greenenergy.backend.service.EnergyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyControllerTest {

    @Mock
    private EnergyService energyService;

    @InjectMocks
    private EnergyController energyController;

    @Test
    void getEnergyMix_ShouldReturnData() {
        when(energyService.getThreeDayEnergyMix()).thenReturn(Collections.emptyList());
        assertNotNull(energyController.getEnergyMix());
    }

    @Test
    void getOptimalChargingWindow_ShouldReturnData() {
        OptimalChargingWindowDto mockDto = OptimalChargingWindowDto.builder()
                .startTime(ZonedDateTime.parse("2026-06-13T10:00:00Z"))
                .endTime(ZonedDateTime.parse("2026-06-13T13:00:00Z"))
                .averageCleanEnergyPercentage(85.5)
                .build();

        when(energyService.getOptimalChargingWindow(3)).thenReturn(mockDto);

        OptimalChargingWindowDto result = energyController.getOptimalChargingWindow(3);
        
        assertNotNull(result);
        assertEquals(85.5, result.getAverageCleanEnergyPercentage());
    }
}
