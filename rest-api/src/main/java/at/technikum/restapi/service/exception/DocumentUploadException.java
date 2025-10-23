package at.technikum.restapi.service.exception;

public class DocumentUploadException extends RuntimeException {
    public DocumentUploadException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DocumentUploadException(final String message) {
        super(message);
    }

    public DocumentUploadException(final Throwable cause) {
        super(cause);
    }
}
