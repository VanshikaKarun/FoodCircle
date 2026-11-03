package com.vkarun.foodcircle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// @ConfigurationPropertiesScan finds the typed config records (e.g. common/JwtProperties) in this package and below.
@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodCircleApplication.class, args);
    }

}
