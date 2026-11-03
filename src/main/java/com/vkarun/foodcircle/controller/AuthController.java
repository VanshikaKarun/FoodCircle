package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.LoginRequest;
import com.vkarun.foodcircle.dto.LoginResponse;
import com.vkarun.foodcircle.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Log in and get an access token",
            description = "Send the token as 'Authorization: Bearer <accessToken>'. Valid for expiresIn seconds. "
                    + "A wrong username and a wrong password both give the same 401.")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
