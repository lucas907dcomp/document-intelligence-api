package com.example.documentintelligence.api;

import com.example.documentintelligence.exception.DocumentNotReadyException;
import com.example.documentintelligence.exception.FileTooLargeException;
import com.example.documentintelligence.exception.InvalidFileTypeException;
import com.example.documentintelligence.exception.LlmUnavailableException;
import com.example.documentintelligence.exception.ResourceNotFoundException;
import com.example.documentintelligence.exception.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({DocumentNotReadyException.class})
    ResponseEntity<ProblemDetail> handleConflict(DocumentNotReadyException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({InvalidFileTypeException.class, FileTooLargeException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    ResponseEntity<ProblemDetail> handleServiceUnavailable(ServiceUnavailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(LlmUnavailableException.class)
    ResponseEntity<ProblemDetail> handleLlmUnavailable(LlmUnavailableException e) {
        return problem(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
