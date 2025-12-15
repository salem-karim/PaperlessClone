package at.technikum.restapi.service;

import at.technikum.restapi.persistence.model.Document;
import at.technikum.restapi.persistence.model.SearchDocument;
import at.technikum.restapi.persistence.repository.SearchDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class ElasticSearchIntegrationTest {

    @Container
    static final ElasticsearchContainer elasticsearchContainer =
        new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.4"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void elasticProps(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @Autowired
    private DocumentSearchService documentSearchService;

    @Autowired
    private SearchDocumentRepository searchDocumentRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void cleanIndex() {
        ensureIndex();
        searchDocumentRepository.deleteAll();
        elasticsearchOperations.indexOps(SearchDocument.class).refresh();
    }

    @Test
    void searchReturnsIndexedDocuments() {
        Document invoice = createDocument("Invoice 1001", "invoice-1001.pdf");
        Document contract = createDocument("Contract 2001", "contract-2001.pdf");

        documentSearchService.indexDocumentMetadata(invoice);
        documentSearchService.indexDocumentMetadata(contract);
        elasticsearchOperations.indexOps(SearchDocument.class).refresh();

        List<SearchDocument> invoiceResults = documentSearchService.search("Invoice");
        List<SearchDocument> contractResults = documentSearchService.search("Contract");

        assertEquals(1, invoiceResults.size());
        assertEquals(invoice.getId(), invoiceResults.getFirst().id());
        assertEquals(1, contractResults.size());
        assertEquals(contract.getId(), contractResults.getFirst().id());
    }

    @Test
    void searchReturnsMultipleDocuments() {
        Document invoice1 = createDocument("Invoice 1001", "invoice-1001.pdf");
        Document invoice2 = createDocument("Invoice 1002", "invoice-1002.pdf");
        Document contract = createDocument("Contract 2001", "contract-2001.pdf");

        documentSearchService.indexDocumentMetadata(invoice1);
        documentSearchService.indexDocumentMetadata(invoice2);
        documentSearchService.indexDocumentMetadata(contract);
        elasticsearchOperations.indexOps(SearchDocument.class).refresh();

        List<SearchDocument> invoiceResults = documentSearchService.search("Invoice");

        assertEquals(2, invoiceResults.size());
        Set<UUID> ids = invoiceResults.stream().map(SearchDocument::id).collect(Collectors.toSet());
        assertTrue(ids.contains(invoice1.getId()));
        assertTrue(ids.contains(invoice2.getId()));
    }

    @Test
    void searchReturnsNoDocumentsForUnknownTerm() {
        Document doc = createDocument("Some Title", "somefile.pdf");

        documentSearchService.indexDocumentMetadata(doc);
        elasticsearchOperations.indexOps(SearchDocument.class).refresh();

        List<SearchDocument> results = documentSearchService.search("NonExistingTermXYZ");
        assertEquals(0, results.size());
    }

    @Test
    void searchFindsInOcrTextAndSummary() {
        // Create a document with unique tokens in OCR and summary text
        Document uniqueDoc = Document.builder()
            .id(UUID.randomUUID())
            .title("Random Title")
            .originalFilename("unique.pdf")
            .contentType("application/pdf")
            .fileSize(1_024L)
            .fileBucket("integration-test")
            .fileObjectKey("unique.pdf")
            .createdAt(Instant.now())
            .processingStatus(Document.ProcessingStatus.COMPLETED)
            .ocrText("this contains uniquetokenocr123")
            .summaryText("this contains uniquesummarytoken456")
            .build();

        documentSearchService.indexDocumentMetadata(uniqueDoc);
        elasticsearchOperations.indexOps(SearchDocument.class).refresh();

        List<SearchDocument> ocrResults = documentSearchService.search("uniquetokenocr123");
        List<SearchDocument> summaryResults = documentSearchService.search("uniquesummarytoken456");

        assertEquals(1, ocrResults.size());
        assertEquals(uniqueDoc.getId(), ocrResults.getFirst().id());
        assertEquals(1, summaryResults.size());
        assertEquals(uniqueDoc.getId(), summaryResults.getFirst().id());
    }

    @Test
    void searchMatchesFilename() {
        Document report1 = createDocument("Yearly Report 2020", "report-2020.pdf");
        Document report2 = createDocument("Summary 2021", "report-2021.pdf");
        Document other = createDocument("Invoice 3001", "invoice-3001.pdf");

        documentSearchService.indexDocumentMetadata(report1);
        documentSearchService.indexDocumentMetadata(report2);
        documentSearchService.indexDocumentMetadata(other);
        elasticsearchOperations.indexOps(SearchDocument.class).refresh();

        List<SearchDocument> results = documentSearchService.search("report");

        assertEquals(2, results.size());
        Set<UUID> ids = results.stream().map(SearchDocument::id).collect(Collectors.toSet());
        assertTrue(ids.contains(report1.getId()));
        assertTrue(ids.contains(report2.getId()));
    }

    private void ensureIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(SearchDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
        }
    }

    private Document createDocument(String title, String originalFilename) {
        return Document.builder()
            .id(UUID.randomUUID())
            .title(title)
            .originalFilename(originalFilename)
            .contentType("application/pdf")
            .fileSize(1_024L)
            .fileBucket("integration-test")
            .fileObjectKey(originalFilename)
            .createdAt(Instant.now())
            .processingStatus(Document.ProcessingStatus.COMPLETED)
            .ocrText("Sample OCR text for " + title)
            .summaryText("Summary for " + title)
            .build();
    }
}
