package com.manadoksli.repository;

import com.manadoksli.model.ImageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ImageDocumentRepository extends ElasticsearchRepository<ImageDocument, String> {
}
