package br.com.tcf5_health_record_transformer.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;

public class MappingRuleRequest {
    // optional; if not a UUID a deterministic UUID will be generated
    private String clientId;

    @NotBlank(message = "joltSpec is required")
    @JsonDeserialize(using = JoltSpecDeserializer.class)
    private String joltSpec;

    public MappingRuleRequest() {}

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJoltSpec() {
        return joltSpec;
    }

    public void setJoltSpec(String joltSpec) {
        this.joltSpec = joltSpec;
    }
}
