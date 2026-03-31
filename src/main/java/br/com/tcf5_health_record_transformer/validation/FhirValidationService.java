package br.com.tcf5_health_record_transformer.validation;

public interface FhirValidationService {
    /**
     * Validate the given FHIR JSON string. If invalid, throws FhirValidationException with details.
     */
    void validateOrThrow(String fhirJson) throws FhirValidationException;
}

