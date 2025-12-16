package at.technikum.restapi.service;

import java.util.List;
import java.util.UUID;

import at.technikum.restapi.service.dto.CategoryDto;

public interface CategoryService {

    CategoryDto upload(final CategoryDto category);

    List<CategoryDto> getAll();

    CategoryDto getById(final UUID id);

    void delete(final UUID id);

    CategoryDto update(final CategoryDto category);
}
