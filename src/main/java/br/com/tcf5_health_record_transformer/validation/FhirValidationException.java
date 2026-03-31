package br.com.tcf5_health_record_transformer.validation;

public class FhirValidationException extends RuntimeException {
    public FhirValidationException(String message) {
        super(message);
    }
}

