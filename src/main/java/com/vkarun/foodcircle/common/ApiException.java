package com.vkarun.foodcircle.common;

import org.springframework.http.HttpStatus;

// A failure the caller should be told about, with the HTTP status that describes it. ApiExceptionHandler turns it into
// an RFC 9457 problem response, so services only say what went wrong and never build HTTP responses themselves.
// One class with a named constructor per case, instead of one tiny subclass per status:  throw ApiException.notFound("...");
public final class ApiException extends RuntimeException {

    private final HttpStatus status;

    private ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // 404: the thing asked for does not exist.
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    // 409: the request is valid but clashes with the current state (a taken username, a closed place).
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    // 400: the request itself is wrong in a way that annotations cannot express (following yourself, a garbled cursor).
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    // 401: we do not know who is calling (wrong login, or a valid token whose user no longer exists).
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
