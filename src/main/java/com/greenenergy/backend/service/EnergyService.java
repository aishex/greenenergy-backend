package com.greenenergy.backend.service;

import com.greenenergy.backend.dto.external.CarbonIntensityResponse;
import com.greenenergy.backend.dto.response.EnergyMixDayDto;
import com.greenenergy.backend.dto.response.OptimalChargingWindowDto;
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

    public OptimalChargingWindowDto getOptimalChargingWindow(int hours) {
        if (hours < 1 || hours > 6) {
            throw new IllegalArgumentException("Hours must be between 1 and 6");
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startSearch = now;
        ZonedDateTime endSearch = now.plusHours(48);

        CarbonIntensityResponse response = carbonApiClient.getGenerationMix(startSearch, endSearch);
        List<CarbonIntensityResponse.CarbonIntensityData> dataList = response.getData();

        if (dataList == null || dataList.isEmpty()) {
            return null;
        }

        int windowSize = hours * 2; // 30 min intervals
        double maxCleanEnergy = -1.0;
        int bestStartIndex = -1;

        double[] cleanEnergyPerInterval = new double[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            double cleanSum = 0.0;
            for (CarbonIntensityResponse.GenerationMix mix : dataList.get(i).getGenerationmix()) {
                if (CLEAN_ENERGY_SOURCES.contains(mix.getFuel())) {
                    cleanSum += mix.getPerc();
                }
            }
            cleanEnergyPerInterval[i] = cleanSum;
        }

        for (int i = 0; i <= dataList.size() - windowSize; i++) {
            double windowSum = 0.0;
            for (int j = 0; j < windowSize; j++) {
                windowSum += cleanEnergyPerInterval[i + j];
            }
            double windowAverage = windowSum / windowSize;
            if (windowAverage > maxCleanEnergy) {
                maxCleanEnergy = windowAverage;
                bestStartIndex = i;
            }
        }

        if (bestStartIndex == -1) {
            return null;
        }

        ZonedDateTime bestStartTime = dataList.get(bestStartIndex).getFrom();
        ZonedDateTime bestEndTime = dataList.get(bestStartIndex + windowSize - 1).getTo();

        return OptimalChargingWindowDto.builder()
                .startTime(bestStartTime)
                .endTime(bestEndTime)
                .averageCleanEnergyPercentage(maxCleanEnergy)
                .build();
    }
}
