package br.com.tcf5_health_record_transformer.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JoltSpecDeserializerTest {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializeArrayShouldProduceStringJson() throws Exception {
        // build payload as tree with joltSpec as array node
        ObjectNode root = mapper.createObjectNode();
        root.put("clientId", "C1");
        ArrayNode arr = mapper.createArrayNode();
        ObjectNode spec = mapper.createObjectNode();
        spec.put("op", "shift");
        ObjectNode specInner = mapper.createObjectNode();
        specInner.put("a", "A");
        spec.set("spec", specInner);
        arr.add(spec);
        root.set("joltSpec", arr);

        MappingRuleRequest req = mapper.treeToValue(root, MappingRuleRequest.class);
        assertNotNull(req.getJoltSpec());

        JsonNode node = mapper.readTree(req.getJoltSpec());
        assertTrue(node.isArray());
        assertEquals("shift", node.get(0).get("op").asText());
        assertEquals("A", node.get(0).get("spec").get("a").asText());
    }

    @Test
    void deserializeStringShouldReturnSameString() throws Exception {
        String specStr = "[{\"op\":\"shift\",\"spec\":{\"x\":\"X\"}}]";
        ObjectNode root = mapper.createObjectNode();
        root.put("clientId", "C2");
        // set joltSpec as text node
        root.put("joltSpec", specStr);

        MappingRuleRequest req = mapper.treeToValue(root, MappingRuleRequest.class);
        assertNotNull(req.getJoltSpec());
        assertEquals(specStr, req.getJoltSpec());

        JsonNode node = mapper.readTree(req.getJoltSpec());
        assertTrue(node.isArray());
        assertEquals("X", node.get(0).get("spec").get("x").asText());
    }
}
