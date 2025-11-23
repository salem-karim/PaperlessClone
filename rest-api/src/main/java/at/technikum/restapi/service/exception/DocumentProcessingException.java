package at.technikum.restapi.service.exception;

public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DocumentProcessingException(final String message) {
        super(message);
    }
}
