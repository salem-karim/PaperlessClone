package at.technikum.restapi.service.exception;

public class DocumentUploadException extends RuntimeException {
    public DocumentUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentUploadException(String message) {
        super(message);
    }

    public DocumentUploadException(Throwable cause) {
        super(cause);
    }
}
