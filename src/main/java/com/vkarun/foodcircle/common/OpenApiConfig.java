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

                        **Logging in:** create a user with `POST /api/users`, then call `POST /api/auth/login`. Send the
                        `accessToken` as `Authorization: Bearer <accessToken>` (valid 1 hour). Reads are public; the feed,
                        friends' reviews, follow/unfollow and posting a review need the token. The caller is always the
                        user inside the token.

                        **Errors** are RFC 9457 problem documents (`application/problem+json`).

                        Place data: © OpenStreetMap contributors (ODbL)."""));
    }
}
