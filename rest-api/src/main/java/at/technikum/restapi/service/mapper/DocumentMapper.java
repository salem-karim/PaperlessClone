package at.technikum.restapi.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.messaging.dto.GenAIRequestDto;
import at.technikum.restapi.service.messaging.dto.OcrRequestDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentSummaryDto toSummaryDto(Document entity);

    DocumentDetailDto toDetailDto(Document entity);

    Document toEntity(DocumentSummaryDto dto);

    Document toEntity(DocumentDetailDto dto);

    @Mapping(target = "documentId", source = "id")
    OcrRequestDto toOcrRequestDto(Document entity);

    @Mapping(target = "documentId", source = "id")
    GenAIRequestDto toGenAIRequestDto(Document entity);

    SearchDocument toSearchDocument(Document entity);

    DocumentSummaryDto toSummaryDto(SearchDocument entity);
}
