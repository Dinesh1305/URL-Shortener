package com.demo.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    private ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ShortCodeGenerator();
    }

    @Test
    @DisplayName("Should generate short code of specified length")
    void testGenerateShortCodeLength() {
        String code6 = generator.generateShortCode(6);
        assertNotNull(code6);
        assertEquals(6, code6.length());

        String code10 = generator.generateShortCode(10);
        assertNotNull(code10);
        assertEquals(10, code10.length());
    }

    @Test
    @DisplayName("Should contain only Base62 characters")
    void testGenerateShortCodeFormat() {
        String code = generator.generateShortCode(6);
        assertTrue(code.matches("^[a-zA-Z0-9]+$"), "Code should be alphanumeric");
    }

    @Test
    @DisplayName("Should generate unique codes across multiple iterations")
    void testUniqueness() {
        Set<String> generatedCodes = new HashSet<>();
        int count = 1000;
        for (int i = 0; i < count; i++) {
            generatedCodes.add(generator.generateShortCode(6));
        }
        assertEquals(count, generatedCodes.size(), "All generated codes should be unique");
    }
}
