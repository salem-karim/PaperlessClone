package at.technikum.restapi.service.mapper;

import at.technikum.restapi.persistence.DocumentEntity;
import at.technikum.restapi.service.DocumentDto;

public class DocumentMapper implements Mapper<DocumentEntity, DocumentDto> {
    @Override
    public DocumentDto toDto(final DocumentEntity entity) {
        return DocumentDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .build();
    }

    @Override
    public DocumentEntity toEntity(final DocumentDto dto) {
        return DocumentEntity.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .build();
    }

    @Override
    public void updateEntityFromDto(DocumentDto updateDoc, DocumentEntity entity) {
        if (updateDoc.getTitle() != null) {
            entity.setTitle(updateDoc.getTitle());
        }
    }
}
