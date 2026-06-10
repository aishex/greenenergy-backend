package com.greenenergy.backend.controller;

import com.greenenergy.backend.dto.response.EnergyMixDayDto;
import com.greenenergy.backend.service.EnergyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EnergyController {

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/energy-mix")
    public List<EnergyMixDayDto> getEnergyMix() {
        return energyService.getThreeDayEnergyMix();
    }
}
