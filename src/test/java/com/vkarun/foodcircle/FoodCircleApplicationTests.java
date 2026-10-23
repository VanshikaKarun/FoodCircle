package com.vkarun.foodcircle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// The test profile points at foodcircle_test, never at the real foodcircle database.
@SpringBootTest
@ActiveProfiles("test")
class FoodCircleApplicationTests {

    @Test
    void contextLoads() {
    }

}
