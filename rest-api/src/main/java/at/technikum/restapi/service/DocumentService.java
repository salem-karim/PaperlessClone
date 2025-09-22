package at.technikum.restapi.service;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    void upload(final DocumentDto doc);

    List<DocumentDto> getAll();

    DocumentDto getById(final UUID id);

    DocumentDto update(final UUID id, final DocumentDto updateDoc);

    void delete(final UUID id);
}
