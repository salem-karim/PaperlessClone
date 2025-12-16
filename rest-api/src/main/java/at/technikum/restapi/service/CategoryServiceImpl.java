package at.technikum.restapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import at.technikum.restapi.persistence.model.Category;
import at.technikum.restapi.persistence.repository.CategoryRepository;
import at.technikum.restapi.service.dto.CategoryDto;
import at.technikum.restapi.service.exception.CategoryAlreadyExistsException;
import at.technikum.restapi.service.exception.CategoryNotFoundException;
import at.technikum.restapi.service.exception.CategoryValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private final CategoryRepository repository;

    @Override
    @Transactional
    public CategoryDto upload(final CategoryDto category) {
        validate(category, false);
        repository.findByNameIgnoreCase(category.name())
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException(category.name());
                });
        final Category saved = repository.save(toEntity(category));
        return toDto(saved);
    }

    @Override
    public List<CategoryDto> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public CategoryDto getById(final UUID id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Override
    @Transactional
    public void delete(final UUID id) {
        if (!repository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Deleted category {}", id);
    }

    @Override
    @Transactional
    public CategoryDto update(final CategoryDto category) {
        validate(category, true);
        final UUID id = category.id();
        final Category existing = repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        if (!existing.getName().equalsIgnoreCase(category.name())) {
            repository.findByNameIgnoreCase(category.name())
                    .ifPresent(other -> {
                        throw new CategoryAlreadyExistsException(category.name());
                    });
        }

        existing.setName(category.name());
        existing.setColor(category.color());
        existing.setIcon(category.icon());

        final Category saved = repository.save(existing);
        return toDto(saved);
    }

private void validate(final CategoryDto category, final boolean requireId) {
    if (category == null) {
        throw new CategoryValidationException("Category payload must not be null");
    }

    var errors = new ArrayList<String>();

    addError(errors, requireId && category.id() == null, "Category id must be provided");
    addError(errors, category.name() == null || category.name().isBlank(), "Category name must not be blank");
    addError(errors, category.name() != null && category.name().length() > 100, "Category name must not exceed 100 characters");
    addError(errors, category.color() == null || !HEX_COLOR_PATTERN.matcher(category.color()).matches(), "Category color must be a hex value like #A1B2C3");
    addError(errors, category.icon() == null || category.icon().isBlank(), "Category icon must not be blank");
    addError(errors, category.icon() != null && category.icon().length() > 50, "Category icon must not exceed 50 characters");

    if (!errors.isEmpty()) {
        throw new CategoryValidationException(String.join("; ", errors));
    }
}

private void addError(final List<String> errors, final boolean condition, final String message) {
    if (condition) {
        errors.add(message);
    }
}

    private Category toEntity(final CategoryDto dto) {
        return Category.builder()
                .id(dto.id())
                .name(dto.name())
                .color(dto.color())
                .icon(dto.icon())
                .build();
    }

    private CategoryDto toDto(final Category entity) {
        return CategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .color(entity.getColor())
                .icon(entity.getIcon())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
