package at.technikum.restapi.service.mapper;

import org.springframework.stereotype.Component;

import at.technikum.restapi.persistence.DocumentEntity;
import at.technikum.restapi.service.DocumentDto;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Component
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
}
