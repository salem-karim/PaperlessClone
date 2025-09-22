package at.technikum.restapi;

import at.technikum.restapi.persistence.DocumentEntity;
import at.technikum.restapi.persistence.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    private DocumentEntity savedDoc;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        savedDoc = repository.save(DocumentEntity.builder()
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
        DocumentEntity newDoc = DocumentEntity.builder()
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
        DocumentEntity updated = DocumentEntity.builder()
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
        DocumentEntity updated = DocumentEntity.builder()
                .title("Does Not Exist")
                .build();

        mockMvc.perform(put("/documents/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateDocument_notFound() throws Exception {
        DocumentEntity updated = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .title("Does Not Exist")
            .build();

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
