package at.technikum.restapi.persistence.repository;

import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import at.technikum.restapi.persistence.model.SearchDocument;

public interface SearchDocumentRepository
        extends ElasticsearchRepository<SearchDocument, UUID> {

}
