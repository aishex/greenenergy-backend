package com.greenenergy.backend.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.greenenergy.backend.dto.external.CarbonIntensityResponse;

@Service
public class CarbonApiClient {

    private final RestTemplate restTemplate;
    private static final String API_URL = "https://api.carbonintensity.org.uk/generation/{from}/{to}";

    public CarbonApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public CarbonIntensityResponse getGenerationMix(ZonedDateTime from, ZonedDateTime to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
        String fromStr = from.withZoneSameInstant(ZoneOffset.UTC).format(formatter);
        String toStr = to.withZoneSameInstant(ZoneOffset.UTC).format(formatter);

        return restTemplate.getForObject(API_URL, CarbonIntensityResponse.class, fromStr, toStr);
    }
}
