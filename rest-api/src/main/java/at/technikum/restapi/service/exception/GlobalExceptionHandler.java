package at.technikum.restapi.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(String message, int status) {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(DocumentNotFoundException.class)
    public ErrorResponse handleNotFound(DocumentNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDocumentException.class)
    public ErrorResponse handleInvalidDocument(InvalidDocumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(DocumentProcessingException.class)
    public ErrorResponse handleProcessingError(DocumentProcessingException ex) {
        log.error("Processing error: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(DocumentUploadException.class)
    public ErrorResponse handleUploadError(DocumentUploadException ex) {
        log.error("Upload error: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericError(Exception ex) {
        log.error("Unhandled exception caught in global handler", ex);
        return new ErrorResponse("An unexpected error occurred. Please contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
