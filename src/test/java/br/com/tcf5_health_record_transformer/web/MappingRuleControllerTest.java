package br.com.tcf5_health_record_transformer.web;

import br.com.tcf5_health_record_transformer.domain.model.MappingRule;
import br.com.tcf5_health_record_transformer.infrastructure.repository.MappingRuleRepository;
import br.com.tcf5_health_record_transformer.web.dto.MappingRuleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MappingRuleControllerTest {

    @Mock
    MappingRuleRepository repository;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void createShouldPersistAndReturnLocation() throws Exception {
        MappingRuleController controller = new MappingRuleController(repository, new ObjectMapper());

        MappingRuleRequest req = new MappingRuleRequest();
        req.setClientId("MY_CLIENT");
        req.setJoltSpec("[{}]");

        when(repository.existsById(any(UUID.class))).thenReturn(false);

        ResponseEntity<?> resp = controller.create(req, false);

        assertEquals(201, resp.getStatusCode().value());
        assertTrue(resp.getHeaders().getLocation().toString().contains("/mapping-rules/"));

        ArgumentCaptor<MappingRule> cap = ArgumentCaptor.forClass(MappingRule.class);
        verify(repository).save(cap.capture());

        MappingRule saved = cap.getValue();
        assertNotNull(saved.getClientId());
    }

    @Test
    void createShouldReturn409WhenExistsAndNoUpsert() throws Exception {
        MappingRuleController controller = new MappingRuleController(repository, new ObjectMapper());

        MappingRuleRequest req = new MappingRuleRequest();
        req.setClientId("MY_CLIENT");
        req.setJoltSpec("[{}]");

        // simulate exists
        when(repository.existsById(any(UUID.class))).thenReturn(true);

        ResponseEntity<?> resp = controller.create(req, false);

        assertEquals(409, resp.getStatusCode().value());
    }

    @Test
    void createShouldUpsertWhenExistsAndUpsertTrue() throws Exception {
        MappingRuleController controller = new MappingRuleController(repository, new ObjectMapper());

        MappingRuleRequest req = new MappingRuleRequest();
        req.setClientId("MY_CLIENT");
        req.setJoltSpec("[{\"op\":\"shift\"}]");

        UUID id = UUID.nameUUIDFromBytes("MY_CLIENT".getBytes());
        MappingRule existing = new MappingRule();
        existing.setClientId(id);
        existing.setJoltSpec("[old]");

        when(repository.existsById(eq(id))).thenReturn(true);
        when(repository.findById(eq(id))).thenReturn(Optional.of(existing));

        ResponseEntity<?> resp = controller.create(req, true);

        assertEquals(200, resp.getStatusCode().value());

        ArgumentCaptor<MappingRule> cap = ArgumentCaptor.forClass(MappingRule.class);
        verify(repository).save(cap.capture());
        MappingRule saved = cap.getValue();
        assertEquals(id, saved.getClientId());
    }
}
