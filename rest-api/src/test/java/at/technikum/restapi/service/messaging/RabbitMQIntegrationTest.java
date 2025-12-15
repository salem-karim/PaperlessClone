package at.technikum.restapi.service.messaging;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.repository.DocumentRepository;
import at.technikum.restapi.service.DocumentSearchService;
import at.technikum.restapi.service.messaging.dto.GenAIRequestDto;
import at.technikum.restapi.service.messaging.dto.GenAIResponseDto;
import at.technikum.restapi.service.messaging.dto.OcrRequestDto;
import at.technikum.restapi.service.messaging.dto.OcrResponseDto;
import at.technikum.restapi.service.messaging.publisher.DocumentPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration"
})
@Testcontainers
@ActiveProfiles("test")
class RabbitMQIntegrationTest {

    @Container
    static final RabbitMQContainer rabbitmqContainer = new RabbitMQContainer("rabbitmq:3-management")
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitmqContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitmqContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmqContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmqContainer::getAdminPassword);
    }

    @Autowired
    private DocumentPublisher documentPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentSearchService documentSearchService;

    private Document testDocument;

    @BeforeEach
    void setup() {
        documentRepository.deleteAll();
        
        // Mock Elasticsearch interactions
        doNothing().when(documentSearchService).indexDocumentMetadata(any());
        doNothing().when(documentSearchService).updateDocumentStatus(any());
        doNothing().when(documentSearchService).updateDocumentAfterOcr(any());
        doNothing().when(documentSearchService).updateDocumentAfterGenAI(any());
        
        testDocument = documentRepository.save(Document.builder()
                .title("Test Document")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .fileBucket("test-bucket")
                .fileObjectKey("test-object-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build());
    }

    // ========== Publisher Tests (REST API -> Workers) ==========

    @Test
    void testPublishDocumentForOcr_success() throws Exception {
        // When - REST API publishes OCR request via exchange with routing key
        documentPublisher.publishDocumentForOcr(testDocument);

        // Then - verify message was sent to OCR queue (bound to exchange with routing key)
        Message message = rabbitTemplate.receive("documents.ocr.processing", 5000);
        assertThat(message).isNotNull();

        OcrRequestDto ocrRequest = objectMapper.readValue(
                message.getBody(), 
                OcrRequestDto.class
        );

        // Verify request matches OcrRequest model from workers
        assertThat(ocrRequest.documentId()).isEqualTo(testDocument.getId().toString());
        assertThat(ocrRequest.fileObjectKey()).isEqualTo(testDocument.getFileObjectKey());
        assertThat(ocrRequest.originalFilename()).isEqualTo(testDocument.getOriginalFilename());
        assertThat(ocrRequest.contentType()).isEqualTo(testDocument.getContentType());
        assertThat(ocrRequest.fileSize()).isEqualTo(testDocument.getFileSize());
        assertThat(ocrRequest.fileBucket()).isEqualTo(testDocument.getFileBucket());
        assertThat(ocrRequest.title()).isEqualTo(testDocument.getTitle());
    }

    @Test
    void testPublishDocumentForGenAI_success() throws Exception {
        // Given - document with OCR text (simulating completed OCR processing)
        testDocument.setOcrText("This is extracted OCR text");
        testDocument.setProcessingStatus(Document.ProcessingStatus.OCR_COMPLETED);
        documentRepository.save(testDocument);

        // When - REST API publishes GenAI request via exchange with routing key
        documentPublisher.publishDocumentForGenAI(testDocument);

        // Then - verify message was sent to GenAI queue
        Message message = rabbitTemplate.receive("documents.genai.processing", 5000);
        assertThat(message).isNotNull();

        GenAIRequestDto genAIRequest = objectMapper.readValue(
                message.getBody(), 
                GenAIRequestDto.class
        );

        // Verify request matches GenAIRequest model from workers
        assertThat(genAIRequest.documentId()).isEqualTo(testDocument.getId().toString());
        assertThat(genAIRequest.ocrText()).isEqualTo("This is extracted OCR text");
    }

    // ========== Consumer Tests (Workers -> REST API) ==========

    @Test
    void testOcrResultConsumer_successWithInlineText() {
        // Given - simulate OCR worker sending completed response with inline text
        // Status is "completed" as per DocumentListenerImpl
        OcrResponseDto ocrResponse = OcrResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .ocrText("Extracted text from OCR worker")
                .ocrTextObjectKey(null)  // No MinIO key for inline text
                .status("completed")
                .worker("ocr-worker-1")
                .build();

        // When - worker publishes response to documents.ocr.processing.response queue
        rabbitTemplate.convertAndSend("documents.ocr.processing.response", ocrResponse);

        // Then - wait for listener to process and verify database update
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
            assertThat(updated.getOcrText()).isEqualTo("Extracted text from OCR worker");
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.OCR_COMPLETED);
            assertThat(updated.getOcrProcessedAt()).isNotNull();
        });
    }

    @Test
    void testOcrResultConsumer_failure() {
        // Given - simulate OCR worker sending failure response
        // Status is "failed" as per DocumentListenerImpl
        OcrResponseDto ocrResponse = OcrResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .ocrText(null)
                .ocrTextObjectKey(null)
                .status("failed")
                .error("OCR processing failed: Unsupported file format")
                .worker("ocr-worker-1")
                .build();

        // When - worker publishes error response
        rabbitTemplate.convertAndSend("documents.ocr.processing.response", ocrResponse);

        // Then - verify error was recorded in database
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.OCR_FAILED);
            assertThat(updated.getProcessingError()).isEqualTo("OCR processing failed: Unsupported file format");
        });
    }

    @Test
    void testOcrResultConsumer_completedButNoTextProvided() {
        // Given - simulate edge case where worker sends completed but no text or reference
        OcrResponseDto ocrResponse = OcrResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .ocrText(null)
                .ocrTextObjectKey(null)
                .status("completed")
                .worker("ocr-worker-1")
                .build();

        // When - worker publishes response
        rabbitTemplate.convertAndSend("documents.ocr.processing.response", ocrResponse);

        // Then - should be marked as failed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.OCR_FAILED);
            assertThat(updated.getProcessingError()).isEqualTo("No OCR text or reference provided");
        });
    }

    @Test
    void testGenAIResultConsumer_success() {
        // Given - document with completed OCR processing
        testDocument.setOcrText("Some OCR text");
        testDocument.setProcessingStatus(Document.ProcessingStatus.OCR_COMPLETED);
        documentRepository.save(testDocument);

        // Simulate GenAI worker sending completed response
        // Status is "completed" as per DocumentListenerImpl
        GenAIResponseDto genAIResponse = GenAIResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .summaryText("AI-generated summary of the document")
                .status("completed")
                .worker("genai-worker-1")
                .build();

        // When - worker publishes response to documents.genai.processing.response queue
        rabbitTemplate.convertAndSend("documents.genai.processing.response", genAIResponse);

        // Then - verify document is fully processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
            assertThat(updated.getSummaryText()).isEqualTo("AI-generated summary of the document");
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
            assertThat(updated.getGenaiProcessedAt()).isNotNull();
        });
    }

    @Test
    void testGenAIResultConsumer_failure() {
        // Given - document with completed OCR processing
        testDocument.setOcrText("Some OCR text");
        testDocument.setProcessingStatus(Document.ProcessingStatus.OCR_COMPLETED);
        documentRepository.save(testDocument);

        // Simulate GenAI worker sending failure response
        // Status is "failed" as per DocumentListenerImpl
        GenAIResponseDto genAIResponse = GenAIResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .summaryText(null)
                .status("failed")
                .error("GenAI processing failed: API rate limit exceeded")
                .worker("genai-worker-1")
                .build();

        // When - worker publishes error response
        rabbitTemplate.convertAndSend("documents.genai.processing.response", genAIResponse);

        // Then - verify error was recorded
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.GENAI_FAILED);
            assertThat(updated.getProcessingError()).isEqualTo("GenAI processing failed: API rate limit exceeded");
        });
    }

    @Test
    void testGenAIResultConsumer_completedButNoSummary() {
        // Given - document with completed OCR processing
        testDocument.setOcrText("Some OCR text");
        testDocument.setProcessingStatus(Document.ProcessingStatus.OCR_COMPLETED);
        documentRepository.save(testDocument);

        // Simulate edge case where worker sends completed but no summary text
        GenAIResponseDto genAIResponse = GenAIResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .summaryText(null)
                .status("completed")
                .worker("genai-worker-1")
                .build();

        // When - worker publishes response
        rabbitTemplate.convertAndSend("documents.genai.processing.response", genAIResponse);

        // Then - should be marked as failed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(testDocument.getId()).orElseThrow();
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.GENAI_FAILED);
            assertThat(updated.getProcessingError()).isEqualTo("No summary text generated");
        });
    }

    // ========== Integration Flow Tests ==========

    @Test
    void testCompleteOcrToGenAIFlow() throws Exception {
        // Given - fresh document
        Document document = documentRepository.save(Document.builder()
                .title("Flow Test Document")
                .originalFilename("flow-test.pdf")
                .contentType("application/pdf")
                .fileSize(54321L)
                .fileBucket("test-bucket")
                .fileObjectKey("flow-test-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build());

        // When - REST API publishes OCR request
        documentPublisher.publishDocumentForOcr(document);

        // Then - verify OCR request message
        Message ocrRequestMsg = rabbitTemplate.receive("documents.ocr.processing", 5000);
        assertThat(ocrRequestMsg).isNotNull();

        // Simulate OCR worker processing and sending response
        OcrResponseDto ocrResponse = OcrResponseDto.builder()
                .documentId(document.getId().toString())
                .ocrText("Extracted text from complete flow")
                .ocrTextObjectKey(null)
                .status("completed")
                .worker("ocr-worker-1")
                .build();
        
        rabbitTemplate.convertAndSend("documents.ocr.processing.response", ocrResponse);

        // Wait for OCR processing to complete and GenAI request to be published
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(document.getId()).orElseThrow();
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.OCR_COMPLETED);
            assertThat(updated.getOcrText()).isNotNull();
        });

        // Verify GenAI request was automatically published
        Message genAIRequestMsg = rabbitTemplate.receive("documents.genai.processing", 5000);
        assertThat(genAIRequestMsg).isNotNull();

        GenAIRequestDto genAIRequest = objectMapper.readValue(
                genAIRequestMsg.getBody(), 
                GenAIRequestDto.class
        );
        assertThat(genAIRequest.documentId()).isEqualTo(document.getId().toString());
        assertThat(genAIRequest.ocrText()).isEqualTo("Extracted text from complete flow");

        // Simulate GenAI worker processing and sending response
        GenAIResponseDto genAIResponse = GenAIResponseDto.builder()
                .documentId(document.getId().toString())
                .summaryText("AI summary from complete flow")
                .status("completed")
                .worker("genai-worker-1")
                .build();

        rabbitTemplate.convertAndSend("documents.genai.processing.response", genAIResponse);

        // Verify final state
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Document finalDoc = documentRepository.findById(document.getId()).orElseThrow();
            assertThat(finalDoc.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.COMPLETED);
            assertThat(finalDoc.getOcrText()).isEqualTo("Extracted text from complete flow");
            assertThat(finalDoc.getSummaryText()).isEqualTo("AI summary from complete flow");
            assertThat(finalDoc.getOcrProcessedAt()).isNotNull();
            assertThat(finalDoc.getGenaiProcessedAt()).isNotNull();
        });
    }

    @Test
    void testOcrFailureDoesNotTriggerGenAI() throws Exception {
        // Given - fresh document
        Document document = documentRepository.save(Document.builder()
                .title("OCR Failure Test")
                .originalFilename("corrupt.pdf")
                .contentType("application/pdf")
                .fileSize(1000L)
                .fileBucket("test-bucket")
                .fileObjectKey("corrupt-key")
                .createdAt(Instant.now())
                .processingStatus(Document.ProcessingStatus.PENDING)
                .build());

        // Simulate OCR worker sending failure response
        OcrResponseDto ocrResponse = OcrResponseDto.builder()
                .documentId(document.getId().toString())
                .ocrText(null)
                .ocrTextObjectKey(null)
                .status("failed")
                .error("File is corrupted")
                .worker("ocr-worker-1")
                .build();

        rabbitTemplate.convertAndSend("documents.ocr.processing.response", ocrResponse);

        // Wait for processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Document updated = documentRepository.findById(document.getId()).orElseThrow();
            assertThat(updated.getProcessingStatus()).isEqualTo(Document.ProcessingStatus.OCR_FAILED);
            assertThat(updated.getProcessingError()).isEqualTo("File is corrupted");
        });

        // Verify GenAI request was NOT published (queue should be empty)
        Thread.sleep(2000); // Give time for any potential message
        Message genAIRequestMsg = rabbitTemplate.receive("documents.genai.processing", 1000);
        assertThat(genAIRequestMsg).isNull();
    }

    // ========== Serialization Tests ==========

    @Test
    void testOcrRequestSerialization() throws Exception {
        OcrRequestDto original = OcrRequestDto.builder()
                .documentId(testDocument.getId().toString())
                .fileObjectKey("test-key")
                .originalFilename("test.pdf")
                .contentType("application/pdf")
                .fileSize(12345L)
                .fileBucket("test-bucket")
                .title("Test Title")
                .build();

        String json = objectMapper.writeValueAsString(original);
        OcrRequestDto deserialized = objectMapper.readValue(json, OcrRequestDto.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void testOcrResponseSerialization() throws Exception {
        OcrResponseDto original = OcrResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .ocrText("Extracted text")
                .ocrTextObjectKey("ocr-results/doc.txt")
                .status("completed")
                .worker("ocr-worker-1")
                .build();

        String json = objectMapper.writeValueAsString(original);
        OcrResponseDto deserialized = objectMapper.readValue(json, OcrResponseDto.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void testGenAIRequestSerialization() throws Exception {
        GenAIRequestDto original = GenAIRequestDto.builder()
                .documentId(testDocument.getId().toString())
                .ocrText("Some extracted text")
                .build();

        String json = objectMapper.writeValueAsString(original);
        GenAIRequestDto deserialized = objectMapper.readValue(json, GenAIRequestDto.class);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    void testGenAIResponseSerialization() throws Exception {
        GenAIResponseDto original = GenAIResponseDto.builder()
                .documentId(testDocument.getId().toString())
                .summaryText("AI summary")
                .status("completed")
                .worker("genai-worker-1")
                .build();

        String json = objectMapper.writeValueAsString(original);
        GenAIResponseDto deserialized = objectMapper.readValue(json, GenAIResponseDto.class);

        assertThat(deserialized).isEqualTo(original);
    }
}