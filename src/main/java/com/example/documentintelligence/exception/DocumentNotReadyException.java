package com.example.documentintelligence.exception;

public class DocumentNotReadyException extends RuntimeException {
    public DocumentNotReadyException(String message) {
        super(message);
    }
}
