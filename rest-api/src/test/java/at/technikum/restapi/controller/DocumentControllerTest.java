package at.technikum.restapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.repository.DocumentRepository;
import at.technikum.restapi.service.DocumentSearchService;
import at.technikum.restapi.service.MinioService;
import at.technikum.restapi.service.dto.DocumentSummaryDto;
import at.technikum.restapi.service.messaging.publisher.DocumentPublisher;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    @MockitoBean
    private MinioService minioService;

    @MockitoBean
    private DocumentPublisher documentPublisher;

    private Document savedDoc;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        doNothing().when(documentSearchService).indexDocumentMetadata(any());
        doNothing().when(documentSearchService).updateDocumentStatus(any());
        doNothing().when(documentSearchService).updateDocumentAfterOcr(any());
        doNothing().when(documentSearchService).updateDocumentAfterGenAI(any());
        doNothing().when(documentSearchService).deleteFromIndex(any());
        doNothing().when(minioService).deleteFile(anyString());
        doNothing().when(documentPublisher).publishDocumentForOcr(any());
        doNothing().when(documentPublisher).publishDocumentForGenAI(any());

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

    // ========== UPLOAD TESTS ==========

    @Test
    void testUploadDocument_success() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes());

        when(minioService.uploadFile(any(MultipartFile.class))).thenReturn("bucket/object-key");

        mockMvc.perform(multipart("/documents")
                .file(file)
                .param("title", "New Document")
                .param("createdAt", String.valueOf(Instant.now().toEpochMilli())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Document"))
                .andExpect(jsonPath("$.originalFilename").value("test.pdf"));

        verify(documentSearchService).indexDocumentMetadata(any());
    }

    @Test
    void testUploadDocument_invalidFileType() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "Test content".getBytes());

        mockMvc.perform(multipart("/documents")
                .file(file)
                .param("title", "Invalid Document")
                .param("createdAt", String.valueOf(Instant.now().toEpochMilli())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUploadDocument_emptyFile() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                new byte[0]);

        mockMvc.perform(multipart("/documents")
                .file(file)
                .param("title", "Empty Document")
                .param("createdAt", String.valueOf(Instant.now().toEpochMilli())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUploadDocument_missingTitle() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Test content".getBytes());

        mockMvc.perform(multipart("/documents")
                .file(file)
                .param("createdAt", String.valueOf(Instant.now().toEpochMilli())))
                .andExpect(status().isBadRequest());
    }

    // ========== GET ALL TESTS ==========

    @Test
    void testGetAllDocuments() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Title"));
    }

    @Test
    void testGetAllDocuments_emptyList() throws Exception {
        repository.deleteAll();

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ========== GET BY ID TESTS ==========

    @Test
    void testGetDocumentById_found() throws Exception {
        mockMvc.perform(get("/documents/" + savedDoc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.id").value(savedDoc.getId().toString()));
    }

    @Test
    void testGetDocumentById_notFound() throws Exception {
        mockMvc.perform(get("/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDocumentById_invalidUUID() throws Exception {
        mockMvc.perform(get("/documents/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ========== OCR STATUS TESTS ==========

    @Test
    void testGetOcrStatus_pending() throws Exception {
        mockMvc.perform(get("/documents/" + savedDoc.getId() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("PENDING"));
    }

    @Test
    void testGetOcrStatus_completed() throws Exception {
        savedDoc.setProcessingStatus(Document.ProcessingStatus.COMPLETED);
        savedDoc.setOcrText("OCR completed text");
        repository.save(savedDoc);

        mockMvc.perform(get("/documents/" + savedDoc.getId() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus").value("COMPLETED"));
    }

    @Test
    void testGetOcrStatus_notFound() throws Exception {
        mockMvc.perform(get("/documents/" + UUID.randomUUID() + "/status"))
                .andExpect(status().isNotFound());
    }

    // ========== DOWNLOAD TESTS ==========

    @Test
    void testDownloadDocument_smallFile_streamDirectly() throws Exception {
        final byte[] content = "Small file content".getBytes();
        when(minioService.downloadFile(savedDoc.getFileObjectKey()))
                .thenReturn(new ByteArrayInputStream(content));

        mockMvc.perform(get("/documents/" + savedDoc.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"" + savedDoc.getOriginalFilename() + "\""))
                .andExpect(content().contentType(savedDoc.getContentType()));

        verify(minioService).downloadFile(savedDoc.getFileObjectKey());
        verify(minioService, never()).generatePresignedUrl(anyString(), anyInt());
    }

    @Test
    void testDownloadDocument_largeFile_returnsPresignedUrl() throws Exception {
        final Document largeDoc = repository.save(Document.builder()
                .title("Large Document")
                .originalFilename("large.pdf")
                .contentType("application/pdf")
                .fileSize(15 * 1024 * 1024L) // 15MB
                .fileBucket("test-bucket")
                .fileObjectKey("large-object-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build());

        when(minioService.generatePresignedUrl(largeDoc.getFileObjectKey(), 15))
                .thenReturn("https://minio.example.com/presigned-url");

        mockMvc.perform(get("/documents/" + largeDoc.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://minio.example.com/presigned-url"))
                .andExpect(jsonPath("$.filename").value("large.pdf"))
                .andExpect(jsonPath("$.fileSize").value(15 * 1024 * 1024L))
                .andExpect(jsonPath("$.expiresIn").value(900));

        verify(minioService).generatePresignedUrl(largeDoc.getFileObjectKey(), 15);
        verify(minioService, never()).downloadFile(anyString());
    }

    @Test
    void testDownloadDocument_notFound() throws Exception {
        mockMvc.perform(get("/documents/" + UUID.randomUUID() + "/download"))
                .andExpect(status().isNotFound());
    }

    // ========== SEARCH TESTS ==========

    @Test
    void testSearchDocuments_withQuery() throws Exception {
        mockMvc.perform(get("/documents/search")
                .param("q", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchDocuments_emptyQuery() throws Exception {
        mockMvc.perform(get("/documents/search")
                .param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testSearchDocuments_missingQueryParam() throws Exception {
        mockMvc.perform(get("/documents/search"))
                .andExpect(status().isBadRequest());
    }

    // ========== UPDATE TESTS ==========

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

        verify(documentSearchService).updateDocumentStatus(any());
    }

    @Test
    void testUpdateDocument_badRequest() throws Exception {
        final DocumentSummaryDto updated = DocumentSummaryDto.builder()
                .id(UUID.randomUUID())
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

    @SuppressWarnings("Annotator")
    @Test
    void testUpdateDocument_invalidJson() throws Exception {
        mockMvc.perform(put("/documents/" + savedDoc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    // ========== DELETE TESTS ==========

    @Test
    void testDeleteDocument() throws Exception {
        mockMvc.perform(delete("/documents/" + savedDoc.getId()))
                .andExpect(status().isNoContent());

        verify(minioService).deleteFile(savedDoc.getFileObjectKey());
        verify(documentSearchService).deleteFromIndex(savedDoc.getId());
    }

    @Test
    void testDeleteDocument_notFound() throws Exception {
        mockMvc.perform(delete("/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDocument_invalidUUID() throws Exception {
        mockMvc.perform(delete("/documents/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }
}
