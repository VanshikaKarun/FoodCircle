package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.dto.PlaceResponse;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;

    public PlaceService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    @Transactional(readOnly = true)
    public PlaceResponse getById(Long id) {
        // Temporary 404; day 6 replaces this with our own exception and the shared ProblemDetail handler.
        return placeRepository.findById(id)
                .map(PlaceResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place " + id + " not found"));
    }
}
