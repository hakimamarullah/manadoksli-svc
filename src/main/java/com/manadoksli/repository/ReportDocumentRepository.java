package com.manadoksli.repository;

import com.manadoksli.model.ReportDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ReportDocumentRepository extends ElasticsearchRepository<ReportDocument, String> {
    long countByImageId(String imageId);
}
