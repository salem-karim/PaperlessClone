package at.technikum.restapi.service.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class DocumentSummaryDto {
    private final UUID id;
    private final String title;
    private final String originalFilename;
    private final Long fileSize;
    private final String contentType;
}
