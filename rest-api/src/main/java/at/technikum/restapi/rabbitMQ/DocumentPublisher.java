package at.technikum.restapi.rabbitMQ;

import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.service.dto.OcrResponseDto;

public interface DocumentPublisher {
    void publishDocumentForOcr(final Document document);

    void publishDocumentForGenAI(final Document document);
}
