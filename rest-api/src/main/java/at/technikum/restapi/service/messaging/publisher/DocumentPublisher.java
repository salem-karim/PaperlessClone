package at.technikum.restapi.service.messaging.publisher;

import at.technikum.restapi.persistence.model.Document;

public interface DocumentPublisher {
    void publishDocumentForOcr(final Document document);

    void publishDocumentForGenAI(final Document document);
}
