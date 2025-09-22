package at.technikum.restapi.service.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Mapper<E, D> {

    D toDto(E source);

    E toEntity(D source);

    void updateEntityFromDto(D updateDoc, E entity);

    public default List<D> toDto(final Collection<E> sources) {
        final List<D> dtos = new ArrayList<>();
        sources.forEach(source -> dtos.add(toDto(source)));
        return dtos;
    }

    public default List<E> toEntity(final Collection<D> sources) {
        final List<E> entities = new ArrayList<>();
        sources.forEach(source -> entities.add(toEntity(source)));
        return entities;
    }

}
