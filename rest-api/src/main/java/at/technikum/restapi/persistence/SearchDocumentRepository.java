package at.technikum.restapi.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SearchDocumentRepository extends ElasticsearchRepository<SearchDocument, UUID> {

    Page<SearchDocument> findByContentContainingIgnoreCase(String query, Pageable pageable);

    Page<SearchDocument> findByTitleContainingIgnoreCase(String query, Pageable pageable);
}
