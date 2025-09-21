package at.technikum.restapi.service.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Mapper<S, T> {

    T toDto(S source);

    S toEntity(T source);

    public default List<T> toDto(final Collection<S> sources) {
        final List<T> dtos = new ArrayList<>();
        sources.forEach(source -> dtos.add(toDto(source)));
        return dtos;
    }

    public default List<S> toEntity(final Collection<T> sources) {
        final List<S> entities = new ArrayList<>();
        sources.forEach(source -> entities.add(toEntity(source)));
        return entities;
    }
}
