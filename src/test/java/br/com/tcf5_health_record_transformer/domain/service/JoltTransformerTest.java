package br.com.tcf5_health_record_transformer.domain.service;

import com.bazaarvoice.jolt.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JoltTransformerTest {

    @Test
    void transformShouldApplySpec() {
        JoltTransformer transformer = new JoltTransformer();

        String inputJson = "{\"name\":\"John\",\"age\":30}";
        String specJson = "[{\"operation\":\"shift\",\"spec\":{\"name\":\"fullName\",\"age\":\"age\"}}]";

        String resultJson = transformer.transform(inputJson, specJson);

        Object result = JsonUtils.jsonToObject(resultJson);
        Object expected = JsonUtils.jsonToObject("{\"fullName\":\"John\",\"age\":30}");

        assertEquals(expected, result);
    }
}

