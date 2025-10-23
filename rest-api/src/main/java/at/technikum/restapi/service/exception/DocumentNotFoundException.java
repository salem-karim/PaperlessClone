package at.technikum.restapi.service.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(final UUID id) {
        super("Document not found: " + id);
    }
}
