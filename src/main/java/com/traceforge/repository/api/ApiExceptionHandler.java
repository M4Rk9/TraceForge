package com.traceforge.repository.api;

import com.traceforge.repository.exception.InvalidRepositoryUrlException;
import com.traceforge.repository.exception.RepositoryCloneException;
import com.traceforge.repository.exception.RepositoryTooLargeException;
import com.traceforge.repository.exception.UnsupportedRepositoryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRepositoryUrlException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRepositoryUrl(
            InvalidRepositoryUrlException exception
    ) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid repository URL", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("The request is invalid.");

        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    @ExceptionHandler(RepositoryTooLargeException.class)
    public ResponseEntity<ProblemDetail> handleRepositoryTooLarge(
            RepositoryTooLargeException exception
    ) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Repository is too large", exception.getMessage());
    }

    @ExceptionHandler(UnsupportedRepositoryException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedRepository(
            UnsupportedRepositoryException exception
    ) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unsupported repository", exception.getMessage());
    }

    @ExceptionHandler(RepositoryCloneException.class)
    public ResponseEntity<ProblemDetail> handleCloneFailure(RepositoryCloneException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Repository clone failed", exception.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}
