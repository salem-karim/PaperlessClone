package at.technikum.restapi.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.OcrRequestDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {
    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    DocumentSummaryDto toSummaryDto(Document entity);

    DocumentDetailDto toDetailDto(Document entity, String downloadUrl, String ocrText);

    Document toEntity(DocumentSummaryDto dto);

    Document toEntity(DocumentDetailDto dto);

    // Map Document entity to OCR request DTO
    @Mapping(target = "documentId", source = "id")
    OcrRequestDto toOcrRequestDto(Document entity);
}
