package at.technikum.restapi.service.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) {
        super("Category with ID '" + id + "' was not found");
    }

    public CategoryNotFoundException(String name) {
        super("Category with name '" + name + "' was not found");
    }
}