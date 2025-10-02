package at.technikum.restapi.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import at.technikum.restapi.persistence.DocumentRepository;
import at.technikum.restapi.service.mapper.DocumentMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;

    @Override
    public DocumentDto upload(final DocumentDto doc) {
        return mapper.toDto(repository.save(mapper.toEntity(doc)));
    }

    @Override
    public List<DocumentDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public DocumentDto getById(final UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
    }

    @Override
    public DocumentDto update(final UUID id, final DocumentDto updateDoc) {
        if (updateDoc.getId() != null && !updateDoc.getId().equals(id)) {
            throw new IllegalArgumentException("ID in path does not match ID in body");
        }
        var entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + id));
        if (updateDoc.getTitle() != null) {
            entity.setTitle(updateDoc.getTitle());
        }
        entity = repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public void delete(final UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Document not found: " + id);
        }
        repository.deleteById(id);
    }

}
