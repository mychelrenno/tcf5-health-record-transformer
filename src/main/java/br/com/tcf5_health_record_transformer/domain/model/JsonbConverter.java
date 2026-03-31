package br.com.tcf5_health_record_transformer.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<String, PGobject> {

    @Override
    public PGobject convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        PGobject pg = new PGobject();
        try {
            pg.setType("jsonb");
            pg.setValue(attribute);
            return pg;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Error converting string to jsonb PGobject", e);
        }
    }

    @Override
    public String convertToEntityAttribute(PGobject dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData.getValue();
    }
}
