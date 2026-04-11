package io.spec0.mockserver.web;

import io.spec0.mockserver.dto.ApiErrorResponse;
import io.spec0.mockserver.error.MockApiException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MockServerRestExceptionHandler {

  @ExceptionHandler(MockApiException.class)
  public ResponseEntity<ApiErrorResponse> handleMockApi(MockApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest()
        .body(new ApiErrorResponse("invalid_argument", ex.getMessage(), List.of()));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiErrorResponse("not_found", ex.getMessage(), List.of()));
  }
}
