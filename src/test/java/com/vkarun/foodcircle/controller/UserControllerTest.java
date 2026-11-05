package com.vkarun.foodcircle.controller;

import com.vkarun.foodcircle.dto.UserResponse;
import com.vkarun.foodcircle.service.UserService;
import org.junit.jupiter.api.Test;
import com.vkarun.foodcircle.common.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // SecurityConfig needs a JwtDecoder bean; tests never decode a real token (jwt() below fakes a logged-in caller).
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserService userService;

    private String json(String username, String displayName) {
        return "{\"username\":" + username + ",\"displayName\":" + displayName + ",\"password\":\"password123\"}";
    }

    @Test
    void create_validRequest_returns201WithTheUser() throws Exception {
        given(userService.create(org.mockito.ArgumentMatchers.any())).willReturn(
                new UserResponse(1L, "asha", "Asha K", 0, 0, Instant.parse("2026-10-27T10:00:00Z")));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json("\"asha\"", "\"Asha K\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("asha"));
    }

    @Test
    void create_invalidRequests_return400() throws Exception {
        String tooLong = "\"" + "a".repeat(31) + "\"";
        for (String body : new String[]{
                json("\"\"", "\"Asha\""),             // blank username
                json(tooLong, "\"Asha\""),            // username longer than 30
                json("\"bad name!\"", "\"Asha\""),    // characters outside letters, digits, underscore
                json("\"asha\"", "\"\""),             // blank display name
                json("null", "\"Asha\""),             // missing username
                "{}"}) {
            mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void follow_usesTheTokenSubjectAsTheCaller_andReturns204() throws Exception {
        mockMvc.perform(post("/api/users/7/follow").with(jwt().jwt(j -> j.subject("3"))))
                .andExpect(status().isNoContent());

        verify(userService).follow(3L, 7L);
    }

    @Test
    void followAndUnfollow_withoutAToken_return401() throws Exception {
        mockMvc.perform(post("/api/users/7/follow")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/7/follow")).andExpect(status().isUnauthorized());
    }

    @Test
    void signUpAndProfile_needNoToken() throws Exception {
        given(userService.getById(1L)).willReturn(new UserResponse(1L, "asha", "Asha K", 0, 0, Instant.now()));

        mockMvc.perform(get("/api/users/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users/1/followers")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());   // reached validation, so security let it through
    }

    @Test
    void unfollow_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/7/follow").with(jwt().jwt(j -> j.subject("3"))))
                .andExpect(status().isNoContent());

        verify(userService).unfollow(3L, 7L);
    }

    @Test
    void get_returnsTheProfile() throws Exception {
        given(userService.getById(1L)).willReturn(new UserResponse(1L, "asha", "Asha K", 2, 3, Instant.now()));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(2))
                .andExpect(jsonPath("$.followingCount").value(3));
    }
}
