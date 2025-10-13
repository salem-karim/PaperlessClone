package at.technikum.restapi.service.exceptions;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(final UUID id) {
        super("Document not found: " + id);
    }
}
