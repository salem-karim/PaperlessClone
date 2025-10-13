package at.technikum.restapi.service.exceptions;

public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
