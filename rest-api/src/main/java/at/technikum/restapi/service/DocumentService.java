package at.technikum.restapi.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    // Existing methods
    DocumentDto upload(final DocumentDto doc);

    List<DocumentDto> getAll();

    DocumentDto getById(final UUID id);

    DocumentDto update(final UUID id, final DocumentDto updateDoc);

    void delete(final UUID id);

    // method for uploading actual files
    DocumentDto upload(final MultipartFile file, final String title);
}