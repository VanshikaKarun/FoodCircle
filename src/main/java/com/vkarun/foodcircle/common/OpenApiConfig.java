package com.vkarun.foodcircle.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// The title and description at the top of Swagger UI (/swagger-ui.html); the endpoints are described from the controllers.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI foodCircleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FoodCircle API")
                .version("0.1.0")
                .description("""
                        Find cafes and restaurants near you in Hyderabad and see what the people you follow thought of them.

                        **Identity is temporary:** endpoints that need to know who you are take an `X-User-Id` header with a user id
                        (create a user first with `POST /api/users`). This is not secure; real authentication comes later.

                        **Errors** are RFC 9457 problem documents (`application/problem+json`).

                        Place data: © OpenStreetMap contributors (ODbL)."""));
    }
}
