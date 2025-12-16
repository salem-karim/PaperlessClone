package at.technikum.restapi.service;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;

import java.util.List;
import java.util.UUID;

public interface DocumentSearchService {

    void indexDocumentMetadata(final Document document);

    void updateDocumentAfterOcr(final Document document);

    void updateDocumentAfterGenAI(final Document document);

    void updateDocumentStatus(final Document document);

    void deleteFromIndex(final UUID documentId);

    List<SearchDocument> search(final String queryString, final List<String> categoryNames);
}
