package com.github.ampie.wiremock.extended;

public class BadMappingException extends IllegalArgumentException {
    public BadMappingException() {
    }

    public BadMappingException(String s) {
        super(s);
    }

    public BadMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadMappingException(Throwable cause) {
        super(cause);
    }
}
