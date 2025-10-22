package at.technikum.restapi.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {
    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    DocumentSummaryDto toSummaryDto(Document entity);

    DocumentDetailDto toDetailDto(Document entity, String downloadUrl, String ocrText);

    Document toEntity(DocumentSummaryDto dto);

    Document toEntity(DocumentDetailDto dto);
}
