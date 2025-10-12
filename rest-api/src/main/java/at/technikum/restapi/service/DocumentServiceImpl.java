package at.technikum.restapi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import at.technikum.restapi.persistence.DocumentRepository;
import at.technikum.restapi.rabbitMQ.DocumentPublisher;
import at.technikum.restapi.service.exceptions.DocumentNotFoundException;
import at.technikum.restapi.service.exceptions.InvalidDocumentException;
import at.technikum.restapi.service.exceptions.DocumentProcessingException;
import at.technikum.restapi.service.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final DocumentPublisher publisher;

    @Override
    public DocumentDto upload(final DocumentDto doc) {
        try {
            publisher.publishDocumentCreated(doc);
            final var saved = repository.save(mapper.toEntity(doc));
            final var dto = mapper.toDto(saved);
            log.info("Document with Title={} successfully uploaded", doc.getTitle());
            return dto;
        } catch (final Exception e) {
            log.error("Failed to upload document Title={}: {}", doc.getTitle(), e.getMessage());
            throw new DocumentProcessingException("Error uploading document: " + doc.getTitle(), e);
        }
    }

    @Override
    public List<DocumentDto> getAll() {
        try {
            final var list = repository.findAll().stream().map(mapper::toDto).toList();
            log.debug("Fetched all documents");
            return list;
        } catch (final DataAccessException e) {
            log.error("Failed to fetch documents: {}", e.getMessage());
            throw new DocumentProcessingException("Error fetching documents", e);
        }
    }

    @Override
    public DocumentDto getById(final UUID id) {
        try {
            final var dto = repository.findById(id)
                    .map(mapper::toDto)
                    .orElseThrow(() -> new DocumentNotFoundException(id));
            log.debug("Fetched document with ID={}", id);
            return dto;
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error accessing document with ID=" + id, e);
        }
    }

    @Override
    public DocumentDto update(final UUID id, final DocumentDto updateDoc) {
        if (updateDoc.getId() != null && !updateDoc.getId().equals(id)) {
            throw new InvalidDocumentException("ID in path does not match ID in body");
        }

        try {
            var entity = repository.findById(id)
                    .orElseThrow(() -> new DocumentNotFoundException(id));

            if (updateDoc.getTitle() != null) {
                entity.setTitle(updateDoc.getTitle());
            }

            entity = repository.save(entity);
            log.info("Document with Title={} & ID={} successfully updated", updateDoc.getTitle(), updateDoc.getId());
            return mapper.toDto(entity);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error updating document with ID=" + id, e);
        }
    }

    @Override
    public void delete(final UUID id) {
        try {
            if (!repository.existsById(id)) {
                throw new DocumentNotFoundException(id);
            }
            repository.deleteById(id);
            log.info("Document with ID={} successfully deleted", id);
        } catch (final DataAccessException e) {
            throw new DocumentProcessingException("Error deleting document with ID=" + id, e);
        }
    }
}
