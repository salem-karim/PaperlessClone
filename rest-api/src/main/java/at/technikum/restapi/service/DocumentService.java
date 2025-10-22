package at.technikum.restapi.service;

import org.springframework.web.multipart.MultipartFile;

import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentSummaryDto upload(final MultipartFile file, final String title);

    List<DocumentSummaryDto> getAll();

    DocumentDetailDto getById(final UUID id);

    DocumentSummaryDto update(final UUID id, final DocumentSummaryDto updateDoc);

    void delete(final UUID id);
}
