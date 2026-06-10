package com.greenenergy.backend.service;

import com.greenenergy.backend.dto.external.CarbonIntensityResponse;
import com.greenenergy.backend.dto.response.EnergyMixDayDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnergyService {

    private final CarbonApiClient carbonApiClient;
    private static final Set<String> CLEAN_ENERGY_SOURCES = Set.of("biomass", "nuclear", "hydro", "wind", "solar");

    public EnergyService(CarbonApiClient carbonApiClient) {
        this.carbonApiClient = carbonApiClient;
    }

    public List<EnergyMixDayDto> getThreeDayEnergyMix() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startOfToday = now.toLocalDate().atStartOfDay(ZoneOffset.UTC);
        ZonedDateTime endOfDayAfterTomorrow = startOfToday.plusDays(2).with(LocalTime.MAX);

        CarbonIntensityResponse response = carbonApiClient.getGenerationMix(startOfToday, endOfDayAfterTomorrow);

        Map<LocalDate, List<CarbonIntensityResponse.CarbonIntensityData>> groupedByDate = response.getData().stream()
                .collect(Collectors.groupingBy(data -> data.getFrom().toLocalDate()));

        List<EnergyMixDayDto> result = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            LocalDate targetDate = startOfToday.toLocalDate().plusDays(i);
            List<CarbonIntensityResponse.CarbonIntensityData> dayData = groupedByDate.getOrDefault(targetDate, Collections.emptyList());
            
            result.add(calculateDayAverage(targetDate, dayData));
        }

        return result;
    }

    private EnergyMixDayDto calculateDayAverage(LocalDate date, List<CarbonIntensityResponse.CarbonIntensityData> dayData) {
        if (dayData.isEmpty()) {
            return EnergyMixDayDto.builder()
                    .date(date)
                    .averageMix(Collections.emptyMap())
                    .cleanEnergyPercentage(0.0)
                    .build();
        }

        Map<String, Double> fuelSum = new HashMap<>();
        for (CarbonIntensityResponse.CarbonIntensityData interval : dayData) {
            for (CarbonIntensityResponse.GenerationMix mix : interval.getGenerationmix()) {
                fuelSum.merge(mix.getFuel(), mix.getPerc(), Double::sum);
            }
        }

        Map<String, Double> averageMix = new HashMap<>();
        double cleanEnergySum = 0.0;
        int intervals = dayData.size();

        for (Map.Entry<String, Double> entry : fuelSum.entrySet()) {
            double avg = entry.getValue() / intervals;
            averageMix.put(entry.getKey(), avg);
            if (CLEAN_ENERGY_SOURCES.contains(entry.getKey())) {
                cleanEnergySum += avg;
            }
        }

        return EnergyMixDayDto.builder()
                .date(date)
                .averageMix(averageMix)
                .cleanEnergyPercentage(cleanEnergySum)
                .build();
    }
}
