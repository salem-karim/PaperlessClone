package at.technikum.restapi.service.exception;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(final String message) {
        super(message);
    }
}
