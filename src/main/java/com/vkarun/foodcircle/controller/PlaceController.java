package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.service.PlaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/{id}")
    public PlaceResponse getPlace(@PathVariable Long id) {
        return placeService.getById(id);
    }
}
