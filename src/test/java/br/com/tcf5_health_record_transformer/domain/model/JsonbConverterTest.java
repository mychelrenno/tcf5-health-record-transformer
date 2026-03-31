package br.com.tcf5_health_record_transformer.domain.model;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class JsonbConverterTest {

    @Test
    void convertToDatabaseColumnShouldReturnPgObjectWithJsonbTypeAndValue() {
        JsonbConverter converter = new JsonbConverter();

        String json = "{\"name\":\"Alice\",\"age\":25}";

        PGobject pg = converter.convertToDatabaseColumn(json);

        assertNotNull(pg);
        assertEquals("jsonb", pg.getType());
        assertEquals(json, pg.getValue());
    }

    @Test
    void convertToDatabaseColumnShouldReturnNullWhenAttributeIsNull() {
        JsonbConverter converter = new JsonbConverter();

        PGobject pg = converter.convertToDatabaseColumn(null);

        assertNull(pg);
    }

    @Test
    void convertToEntityAttributeShouldReturnValueFromPgObject() throws SQLException {
        JsonbConverter converter = new JsonbConverter();

        PGobject pg = new PGobject();
        pg.setType("jsonb");
        String json = "{\"ok\":true}";
        pg.setValue(json);

        String result = converter.convertToEntityAttribute(pg);

        assertEquals(json, result);
    }

    @Test
    void convertToEntityAttributeShouldReturnNullWhenDbDataIsNull() {
        JsonbConverter converter = new JsonbConverter();

        String result = converter.convertToEntityAttribute(null);

        assertNull(result);
    }
}
