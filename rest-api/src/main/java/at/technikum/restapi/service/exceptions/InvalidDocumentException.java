package at.technikum.restapi.service.exceptions;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(final String message) {
        super(message);
    }
}
