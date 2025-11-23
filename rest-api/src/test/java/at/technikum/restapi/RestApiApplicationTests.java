package at.technikum.restapi;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.repository.DocumentRepository;
import at.technikum.restapi.service.DocumentSearchService;
import at.technikum.restapi.service.MinioService;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.messaging.publisher.DocumentPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock external services
    @MockitoBean
    private DocumentPublisher documentPublisher;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    @MockitoBean
    private MinioService minioService; // ← Add this to prevent MinIO connection attempts

    private Document savedDoc;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        // Mock all external service calls
        doNothing().when(documentPublisher).publishDocumentForOcr(any());
        doNothing().when(documentPublisher).publishDocumentForGenAI(any());
        doNothing().when(documentSearchService).indexDocumentMetadata(any());
        doNothing().when(documentSearchService).deleteFromIndex(any());
        doNothing().when(minioService).deleteFile(anyString()); // ← Mock MinIO delete

        savedDoc = repository.save(Document.builder()
                .title("Test Title")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .fileBucket("test-bucket")
                .fileObjectKey("test-object-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build());
    }

    @Test
    void testGetAllDocuments() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    void testGetDocumentById_found() throws Exception {
        mockMvc.perform(get("/documents/" + savedDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title"));
    }

    @Test
    void testGetDocumentById_notFound() throws Exception {
        mockMvc.perform(get("/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateDocument_found() throws Exception {
        final DocumentSummaryDto updated = DocumentSummaryDto.builder()
                .id(savedDoc.getId())
                .title("Updated Title")
                .originalFilename(savedDoc.getOriginalFilename())
                .contentType(savedDoc.getContentType())
                .fileSize(savedDoc.getFileSize())
                .processingStatus(savedDoc.getProcessingStatus())
                .createdAt(savedDoc.getCreatedAt())
                .build();

        mockMvc.perform(put("/documents/" + savedDoc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void testUpdateDocument_badRequest() throws Exception {
        final DocumentSummaryDto updated = DocumentSummaryDto.builder()
                .id(UUID.randomUUID()) // Mismatched ID
                .title("Mismatched ID")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .processingStatus(Document.ProcessingStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        mockMvc.perform(put("/documents/" + savedDoc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateDocument_notFound() throws Exception {
        final UUID randomId = UUID.randomUUID();
        final DocumentSummaryDto updated = DocumentSummaryDto.builder()
                .id(randomId)
                .title("Does Not Exist")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .processingStatus(Document.ProcessingStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        mockMvc.perform(put("/documents/" + randomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDocument() throws Exception {
        mockMvc.perform(delete("/documents/" + savedDoc.getId()))
                .andExpect(status().isNoContent());
    }
}
