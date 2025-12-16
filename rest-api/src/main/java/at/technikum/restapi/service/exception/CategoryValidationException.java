package at.technikum.restapi.service.exception;

public class CategoryValidationException extends RuntimeException {

    public CategoryValidationException(String message) {
        super(message);
    }
}