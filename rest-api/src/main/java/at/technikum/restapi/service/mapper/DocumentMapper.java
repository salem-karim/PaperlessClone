package at.technikum.restapi.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.dto.OcrStatusDto;
import at.technikum.restapi.service.messaging.dto.GenAIRequestDto;
import at.technikum.restapi.service.messaging.dto.OcrRequestDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentSummaryDto toSummaryDto(final Document entity);

    DocumentSummaryDto toSummaryDto(final SearchDocument entity);

    DocumentDetailDto toDetailDto(final Document entity);

    OcrStatusDto toOcrStatusDto(final Document entity);

    @Mapping(target = "documentId", source = "id")
    OcrRequestDto toOcrRequestDto(final Document entity);

    default GenAIRequestDto toGenAIRequestDto(final Document entity, final String ocrText) {
        return GenAIRequestDto.builder()
                .documentId(entity.getId().toString())
                .ocrText(ocrText)
                .build();
    }

    @Mapping(target = "processingStatus", expression = "java(document.getProcessingStatus().name())")
    SearchDocument toSearchDocument(final Document document);
}
