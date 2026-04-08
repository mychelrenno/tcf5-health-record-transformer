package br.com.tcf5_health_record_transformer.web;

import br.com.tcf5_health_record_transformer.domain.model.MappingRule;
import br.com.tcf5_health_record_transformer.infrastructure.repository.MappingRuleRepository;
import br.com.tcf5_health_record_transformer.web.dto.MappingRuleRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/mapping-rules")
public class MappingRuleController {
    private final MappingRuleRepository repository;
    private final ObjectMapper objectMapper;

    // Single constructor - Spring will autowire it automatically
    public MappingRuleController(MappingRuleRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody MappingRuleRequest request,
                                    @RequestParam(name = "upsert", defaultValue = "false") boolean upsert) throws JsonProcessingException {
        // validate joltSpec JSON
        try {
            if (!hasJoltSpec(request)) return ResponseEntity.badRequest().body("joltSpec is required and cannot be empty");

            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(request.getJoltSpec()));
            if (node == null) throw new IllegalArgumentException("joltSpec is empty or invalid");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid joltSpec JSON: " + ex.getMessage());
        }

        String clientIdStr = request.getClientId();
        UUID clientId;
        if (clientIdStr == null || clientIdStr.isBlank()) {
            clientId = UUID.randomUUID();
        } else {
            try {
                clientId = UUID.fromString(clientIdStr);
            } catch (IllegalArgumentException ex) {
                clientId = UUID.nameUUIDFromBytes(clientIdStr.getBytes());
            }
        }

        boolean exists = repository.existsById(clientId);
        if (exists && !upsert) {
            return ResponseEntity.status(409).body("MappingRule with clientId already exists: " + clientId);
        }

        if (exists) {
            // upsert -> update existing
            Optional<MappingRule> maybe = repository.findById(clientId);
            MappingRule mr = maybe.orElseGet(MappingRule::new);
            mr.setClientId(clientId);
            mr.setJoltSpec(objectMapper.writeValueAsString(request.getJoltSpec()));
            repository.save(mr);
            return ResponseEntity.ok(clientId.toString());
        } else {
            MappingRule mr = new MappingRule();
            mr.setClientId(clientId);
            mr.setJoltSpec(objectMapper.writeValueAsString(request.getJoltSpec()));
            repository.save(mr);
            return ResponseEntity.created(URI.create("/mapping-rules/" + clientId)).body(clientId.toString());
        }
    }

    @GetMapping
    public ResponseEntity<List<MappingRule>> listAll() {
        List<MappingRule> all = repository.findAll();
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<?> getById(@PathVariable String clientId) {
        UUID id = parseClientId(clientId);
        Optional<MappingRule> maybe = repository.findById(id);
        return maybe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<?> update(@PathVariable String clientId, @Valid @RequestBody MappingRuleRequest request) throws JsonProcessingException {
        // validate joltSpec JSON
        try {
            if (!hasJoltSpec(request)) return ResponseEntity.badRequest().body("joltSpec is required and cannot be empty");

            JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(request.getJoltSpec()));
            if (node == null) throw new IllegalArgumentException("joltSpec is empty or invalid");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid joltSpec JSON: " + ex.getMessage());
        }

        UUID id = parseClientId(clientId);
        Optional<MappingRule> maybe = repository.findById(id);
        if (maybe.isEmpty()) {
            // upsert behavior: create new if not exists
            MappingRule mr = new MappingRule();
            mr.setClientId(id);
            mr.setJoltSpec(objectMapper.writeValueAsString(request.getJoltSpec()));
            repository.save(mr);
            return ResponseEntity.created(URI.create("/mapping-rules/" + id)).body(mr);
        }
        MappingRule mr = maybe.get();
        mr.setJoltSpec(objectMapper.writeValueAsString(request.getJoltSpec()));
        repository.save(mr);
        return ResponseEntity.ok(mr);
    }

    private static Boolean hasJoltSpec(MappingRuleRequest request) {
        if (request.getJoltSpec() == null ||
                (request.getJoltSpec() instanceof Collection && ((Collection<?>) request.getJoltSpec()).isEmpty()) ||
                (request.getJoltSpec() instanceof Map && ((Map<?,?>) request.getJoltSpec()).isEmpty())) {
            return false;
        }
        return true;
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<?> delete(@PathVariable String clientId) {
        UUID id = parseClientId(clientId);
        boolean exists = repository.existsById(id);
        if (!exists) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UUID parseClientId(String clientIdStr) {
        if (clientIdStr == null) return UUID.randomUUID();
        try {
            return UUID.fromString(clientIdStr);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(clientIdStr.getBytes());
        }
    }
}
