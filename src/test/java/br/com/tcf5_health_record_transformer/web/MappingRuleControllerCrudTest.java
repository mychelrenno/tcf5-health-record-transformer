package br.com.tcf5_health_record_transformer.web;

import br.com.tcf5_health_record_transformer.domain.model.MappingRule;
import br.com.tcf5_health_record_transformer.infrastructure.repository.MappingRuleRepository;
import br.com.tcf5_health_record_transformer.web.dto.MappingRuleRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MappingRuleControllerCrudTest {

    @Mock
    MappingRuleRepository repository;

    @Test
    void listAllShouldReturnAll() {
        MappingRuleController controller = new MappingRuleController(repository, new com.fasterxml.jackson.databind.ObjectMapper());
        MappingRule a = new MappingRule(UUID.randomUUID(), "[a]");
        MappingRule b = new MappingRule(UUID.randomUUID(), "[b]");

        when(repository.findAll()).thenReturn(List.of(a,b));

        ResponseEntity<List<MappingRule>> resp = controller.listAll();
        assertEquals(2, resp.getBody().size());
    }

    @Test
    void getByIdFoundAndNotFound() {
        MappingRuleController controller = new MappingRuleController(repository, new com.fasterxml.jackson.databind.ObjectMapper());
        UUID id = UUID.randomUUID();
        MappingRule mr = new MappingRule(id, "[]");

        when(repository.findById(eq(id))).thenReturn(Optional.of(mr));
        ResponseEntity<?> ok = controller.getById(id.toString());
        assertEquals(200, ok.getStatusCode().value());

        when(repository.findById(eq(UUID.nameUUIDFromBytes("notfound".getBytes())))).thenReturn(Optional.empty());
        ResponseEntity<?> notfound = controller.getById("notfound");
        assertEquals(404, notfound.getStatusCode().value());
    }

    @Test
    void updateExistingAndNotFound() throws JsonProcessingException {
        MappingRuleController controller = new MappingRuleController(repository, new com.fasterxml.jackson.databind.ObjectMapper());
        UUID id = UUID.randomUUID();
        MappingRule existing = new MappingRule(id, "[old]");

        when(repository.findById(eq(id))).thenReturn(Optional.of(existing));

        MappingRuleRequest req = new MappingRuleRequest();
        req.setJoltSpec("[{\"op\":\"update\"}]");

        ResponseEntity<?> resp = controller.update(id.toString(), req);
        assertEquals(200, resp.getStatusCode().value());
        ArgumentCaptor<MappingRule> cap = ArgumentCaptor.forClass(MappingRule.class);
        verify(repository).save(cap.capture());
        when(repository.findById(eq(UUID.nameUUIDFromBytes("nope".getBytes())))).thenReturn(Optional.empty());

        ResponseEntity<?> created = controller.update("nope", req);
        assertEquals(201, created.getStatusCode().value());
    }

    @Test
    void deleteExistingAndNotFound() {
        MappingRuleController controller = new MappingRuleController(repository, new com.fasterxml.jackson.databind.ObjectMapper());
        UUID id = UUID.randomUUID();

        when(repository.existsById(eq(id))).thenReturn(true);
        ResponseEntity<?> ok = controller.delete(id.toString());
        assertEquals(204, ok.getStatusCode().value());
        verify(repository).deleteById(eq(id));

        when(repository.existsById(eq(UUID.nameUUIDFromBytes("nope".getBytes())))).thenReturn(false);
        ResponseEntity<?> notfound = controller.delete("nope");
        assertEquals(404, notfound.getStatusCode().value());
    }
}
