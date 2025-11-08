package at.technikum.restapi.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.GenAIRequestDto;
import at.technikum.restapi.service.dto.OcrRequestDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentSummaryDto toSummaryDto(Document entity);

    @Mapping(target = "downloadUrl", source = "downloadUrl")
    @Mapping(target = "ocrText", source = "ocrText")
    @Mapping(target = "ocrTextDownloadUrl", ignore = true)
    DocumentDetailDto toDetailDto(Document entity, String downloadUrl, String ocrText);

    Document toEntity(DocumentSummaryDto dto);

    Document toEntity(DocumentDetailDto dto);

    @Mapping(target = "documentId", source = "id")
    OcrRequestDto toOcrRequestDto(Document entity);

    @Mapping(target = "documentId", source = "id")
    GenAIRequestDto toGenAIRequestDto(Document entity);
}
