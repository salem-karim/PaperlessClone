package at.technikum.restapi.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;

    private String originalFilename;

    private String contentType;

    private Long fileSize; // bytes

    // key or path in MinIO (e.g. "paperless-files/<uuid>-file.pdf")
    private String objectKey;

    @Column(updatable = false)
    private Instant createdAt;

    @Lob
    private String ocrTextRef; // could be MinIO key or plain text
}
