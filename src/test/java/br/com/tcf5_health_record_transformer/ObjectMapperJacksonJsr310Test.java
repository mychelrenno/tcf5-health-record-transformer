package br.com.tcf5_health_record_transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectMapperJacksonJsr310Test {

    @Test
    void objectMapperShouldSerializeAndDeserializeLocalDateTime() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        LocalDateTime now = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

        String serialized = objectMapper.writeValueAsString(now);

        // Deserialize back
        LocalDateTime parsed = objectMapper.readValue(serialized, LocalDateTime.class);

        assertEquals(now, parsed);
    }
}
