package com.vkarun.foodcircle.common;

import com.vkarun.foodcircle.controller.FeedController;
import com.vkarun.foodcircle.controller.UserController;
import com.vkarun.foodcircle.service.FeedService;
import com.vkarun.foodcircle.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every error the API can produce must be an RFC 9457 problem document: application/problem+json with status, title and detail.
@WebMvcTest({UserController.class, FeedController.class})
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FeedService feedService;

    private ResultActions expectProblem(ResultActions result, int status, String title) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void ourExceptions_becomeProblemsWithTheirOwnStatusAndMessage() throws Exception {
        given(userService.getById(1L)).willThrow(ApiException.notFound("User 1 not found"));
        given(userService.getById(2L)).willThrow(ApiException.conflict("Username taken"));
        given(userService.getById(3L)).willThrow(ApiException.unauthorized("Unknown user in X-User-Id"));
        given(userService.getById(4L)).willThrow(ApiException.badRequest("You cannot follow yourself"));

        expectProblem(mockMvc.perform(get("/api/users/1")), 404, "Not Found").andExpect(jsonPath("$.detail").value("User 1 not found"));
        expectProblem(mockMvc.perform(get("/api/users/2")), 409, "Conflict").andExpect(jsonPath("$.detail").value("Username taken"));
        expectProblem(mockMvc.perform(get("/api/users/3")), 401, "Unauthorized");
        expectProblem(mockMvc.perform(get("/api/users/4")), 400, "Bad Request");
    }

    @Test
    void aDatabaseConstraintViolation_is409_withoutLeakingTheSql() throws Exception {
        given(userService.getById(1L)).willThrow(new DataIntegrityViolationException(
                "could not execute statement [ERROR: duplicate key value violates unique constraint \"uk_secret_name\"]"));

        expectProblem(mockMvc.perform(get("/api/users/1")), 409, "Conflict")
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("uk_secret_name"))));
    }

    @Test
    void anUnexpectedException_is500_withNoInternalsInTheBody() throws Exception {
        given(userService.getById(1L)).willThrow(new IllegalStateException("password is hunter2 and the table is called users"));

        expectProblem(mockMvc.perform(get("/api/users/1")), 500, "Internal Server Error")
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hunter2"))));
    }

    @Test
    void aBadBody_is400_andListsTheFailingFields() throws Exception {
        expectProblem(mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bad name!\",\"displayName\":\"\"}")), 400, "Bad Request")
                .andExpect(jsonPath("$.errors[?(@.field == 'username')].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'displayName')].message").isNotEmpty());
    }

    @Test
    void aBadParameter_is400_andNamesTheParameter() throws Exception {
        expectProblem(mockMvc.perform(get("/api/feed").header("X-User-Id", "1").param("limit", "0")), 400, "Bad Request")
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
    }

    @Test
    void springsOwnFailures_areProblemsToo() throws Exception {
        // malformed JSON
        expectProblem(mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{not json")), 400, "Bad Request");
        // a missing header
        expectProblem(mockMvc.perform(get("/api/feed")), 400, "Bad Request");
        // an id that is not a number
        expectProblem(mockMvc.perform(get("/api/users/abc")), 400, "Bad Request");
        // a URL that does not exist
        expectProblem(mockMvc.perform(get("/api/nothing-here")), 404, "Not Found");
        // a method the URL does not support
        expectProblem(mockMvc.perform(put("/api/users/1")), 405, "Method Not Allowed");
        // a body in a format we do not accept
        expectProblem(mockMvc.perform(post("/api/users").contentType(MediaType.TEXT_PLAIN).content("hello")), 415, "Unsupported Media Type");
    }
}
