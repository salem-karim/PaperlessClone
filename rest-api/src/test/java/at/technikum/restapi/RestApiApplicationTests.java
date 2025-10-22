package at.technikum.restapi;

import at.technikum.restapi.persistence.Document;
import at.technikum.restapi.persistence.DocumentRepository;
import at.technikum.restapi.rabbitMQ.DocumentPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentPublisher documentPublisher;

    private Document savedDoc;

    @BeforeEach
    void setup() {
        repository.deleteAll();

        // Mock the publisher so it doesn't actually try to send messages
        doNothing().when(documentPublisher).publishDocumentCreated(any());

        savedDoc = repository.save(Document.builder()
                .title("Test Title")
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
    void testUploadDocument() throws Exception {
        Document newDoc = Document.builder()
                .title("New Document")
                .build();

        mockMvc.perform(post("/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDoc)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Document"));
    }

    @Test
    void testUpdateDocument_found() throws Exception {
        Document updated = Document.builder()
                .title("Updated Title")
                .build();

        mockMvc.perform(put("/documents/" + savedDoc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void testUpdateDocument_badRequest() throws Exception {
        Document updated = Document.builder()
                .id(UUID.randomUUID())
                .title("Mismatched ID")
                .build();

        mockMvc.perform(put("/documents/" + savedDoc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateDocument_notFound() throws Exception {
        Document updated = Document.builder()
                .id(UUID.randomUUID())
                .title("Does Not Exist")
                .build();

        // Use ID from created Entity for Not Found Error
        mockMvc.perform(put("/documents/" + updated.getId())
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
