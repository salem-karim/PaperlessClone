package at.technikum.restapi.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import at.technikum.restapi.persistence.model.Category;
import at.technikum.restapi.service.dto.CategoryDto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    Category toEntity(final CategoryDto dto);

    CategoryDto toDto(final Category entity);
}
