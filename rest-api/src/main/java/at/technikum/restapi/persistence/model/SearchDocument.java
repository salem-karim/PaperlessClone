package at.technikum.restapi.persistence.model;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Builder;

@Builder
@Document(indexName = "documents")
public record SearchDocument(
        @Id UUID id,

        // Full-text searchable fields (analyzed, tokenized)
        @Field(type = FieldType.Text, analyzer = "standard") String title,

        @Field(type = FieldType.Text, analyzer = "standard") String originalFilename,

        @Field(type = FieldType.Text, analyzer = "standard") String ocrText,

        @Field(type = FieldType.Text, analyzer = "standard") String summaryText,

        // Numeric field for filtering/sorting
        @Field(type = FieldType.Long) Long fileSize,

        // Keyword fields (exact match, good for filters)
        @Field(type = FieldType.Keyword) String contentType,
        // Note: Stored as String, not enum
        @Field(type = FieldType.Keyword) String processingStatus,

        // Date fields for time-based queries
        @Field(type = FieldType.Date) Instant createdAt) {
}
