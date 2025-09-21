package at.technikum.restapi.service;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class DocumentDto {
    private final UUID id;
    private final String title;
}
