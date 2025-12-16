package at.technikum.restapi.service.mapper;

import at.technikum.restapi.persistence.model.Category;
import at.technikum.restapi.service.dto.WorkerStatusDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.service.dto.DocumentDetailDto;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.messaging.dto.GenAIRequestDto;
import at.technikum.restapi.service.messaging.dto.OcrRequestDto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    DocumentSummaryDto toSummaryDto(final Document entity);

    DocumentSummaryDto toSummaryDto(final SearchDocument entity);

    DocumentDetailDto toDetailDto(final Document entity);

    WorkerStatusDto toWorkerStatusDto(final Document entity);

    @Mapping(target = "documentId", source = "id")
    OcrRequestDto toOcrRequestDto(final Document entity);

    default GenAIRequestDto toGenAIRequestDto(final Document entity, final String ocrText) {
        return GenAIRequestDto.builder()
                .documentId(entity.getId().toString())
                .ocrText(ocrText)
                .build();
    }

    @Mapping(target = "categoryNames", expression = "java(mapCategoryNames(document.getCategories()))")
    SearchDocument toSearchDocument(Document document);

    default List<String> mapCategoryNames(final Collection<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        return categories.stream()
            .map(Category::getName)
            .collect(Collectors.toList());
    }

}
