package me.bristermitten.mittenlib.annotations.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import me.bristermitten.mittenlib.annotations.integration.IntegrationTest;

public class IntegrationTests {
    public static String loadResourceString(String resource) throws IOException {
        try (var res = IntegrationTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (res == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(res.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
