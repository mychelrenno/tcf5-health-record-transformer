package br.com.tcf5_health_record_transformer.web.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class JoltSpecDeserializer extends StdDeserializer<String> {

    private final ObjectMapper mapper = new ObjectMapper();

    public JoltSpecDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING) {
            String txt = p.getText();
            // Validate JSON string: if it's actually JSON, keep as-is; otherwise return as-is (controller will validate)
            return txt;
        }
        // If the token is start object/array or other, read as tree and serialize
        JsonNode node = p.readValueAsTree();
        return mapper.writeValueAsString(node);
    }
}

