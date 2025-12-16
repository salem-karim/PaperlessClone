package at.technikum.restapi.persistence.model;

import java.time.Instant;
import java.util.List;
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
        @Field(type = FieldType.Text, analyzer = "standard") String title,
        @Field(type = FieldType.Text, analyzer = "standard") String originalFilename,
        @Field(type = FieldType.Text, analyzer = "standard") String ocrText,
        @Field(type = FieldType.Text, analyzer = "standard") String summaryText,
        @Field(type = FieldType.Long) Long fileSize,
        @Field(type = FieldType.Keyword) String contentType,
        @Field(type = FieldType.Keyword) String processingStatus,
        @Field(type = FieldType.Date) Instant createdAt,
        @Field(type = FieldType.Keyword, normalizer = "lowercase") List<String> categoryNames) {
}