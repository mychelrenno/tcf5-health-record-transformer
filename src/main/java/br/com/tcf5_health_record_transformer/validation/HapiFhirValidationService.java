package br.com.tcf5_health_record_transformer.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class HapiFhirValidationService implements FhirValidationService {
    private static final Logger log = LoggerFactory.getLogger(HapiFhirValidationService.class);
    private final FhirContext ctx;
    private final FhirValidator validator;

    public HapiFhirValidationService() {
        // Usando R4
        this.ctx = FhirContext.forR4();
        this.validator = ctx.newValidator();
    }

    @Override
    public void validateOrThrow(String fhirJson) throws FhirValidationException {
        try {
            IParser parser = ctx.newJsonParser();
            IBaseResource resource = (IBaseResource) parser.parseResource(fhirJson);

            ValidationResult result = validator.validateWithResult(resource);
            if (!result.isSuccessful()) {
                String msgs = result.getMessages().stream()
                        .map(m -> m.getSeverity() + " - " + m.getLocationString() + " - " + m.getMessage())
                        .collect(Collectors.joining("\n"));
                log.warn("FHIR validation errors:\n{}", msgs);
                throw new FhirValidationException(msgs);
            }
        } catch (FhirValidationException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error parsing/validating FHIR resource", ex);
            throw new FhirValidationException("Error parsing/validating FHIR resource: " + ex.getMessage());
        }
    }
}

