package at.technikum.restapi.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.OcrStatusDto;

public interface DocumentService {

    DocumentSummaryDto upload(final MultipartFile file, final String title, final Instant createdAt);

    List<DocumentSummaryDto> getAll();

    DocumentDetailDto getById(final UUID id);

    OcrStatusDto getOcrStatus(final UUID id);

    DocumentSummaryDto update(final UUID id, final DocumentSummaryDto updateDoc);

    void delete(final UUID id);

    void updateOcrResult(final UUID documentId, final String ocrText, final String ocrTextObjectKey);

    void markOcrAsFailed(final UUID documentId, final String error);

    void updateGenAIResult(final UUID documentId, final String summaryText);

    void markGenAIAsFailed(final UUID documentId, final String error);

    /**
     * Volltextsuche über Dokumente (Elasticsearch).
     */
    List<DocumentSummaryDto> search(final String query, final int page, final int size);
}
