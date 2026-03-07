package com.manadoksli.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.manadoksli.dto.ApiResponse;
import com.manadoksli.dto.PagedResult;
import com.manadoksli.dto.SearchReq;
import com.manadoksli.dto.SearchResult;
import com.manadoksli.model.ImageDocument;
import com.manadoksli.repository.ImageDocumentRepository;
import com.manadoksli.service.IImageService;
import com.manadoksli.service.IOcrService;
import com.manadoksli.service.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
@RegisterReflectionForBinding({
        ApiResponse.class,
        SortOptions.class,
        PagedResult.class,
        ImageDocument.class,
        SearchResult.class,
        SearchReq.class
})
public class ImageSvc implements IImageService {

    private final IOcrService ocrService;
    private final IStorageService storageService;
    private final ImageDocumentRepository imageDocumentRepository;
    private final ElasticsearchClient elasticsearchClient;

    private static final double DUPLICATE_THRESHOLD = 0.85;

    @Transactional
    @Override
    public ApiResponse<String> upload(MultipartFile file) {
        var extractedText = ocrService.extractText(file);

        if (extractedText.isBlank()) {
            return ApiResponse.setResponse(null, "No text could be extracted from the image", 400);
        }

        if (isDuplicate(extractedText)) {
            return ApiResponse.setResponse(null, "Similar image already exists", 409);
        }

        var imageUrl = storageService.upload(file);

        imageDocumentRepository.save(ImageDocument.builder()
                .id(UUID.randomUUID().toString())
                .text(extractedText)
                .imageUrl(imageUrl)
                .uploadedAt(OffsetDateTime.now())
                .build());
        log.info("Image indexed successfully: {}", imageUrl);

        return ApiResponse.setSuccess(imageUrl);
    }


    @Override
    public ApiResponse<PagedResult<SearchResult>> search(SearchReq req) {
        try {
            var hasQuery = req.getQuery() != null && !req.getQuery().isBlank();

            // Build sort options
            var sorts = new ArrayList<SortOptions>();
            if (hasQuery) {
                sorts.add(SortOptions.of(so -> so.score(sc -> sc.order(SortOrder.Desc))));
            }
            sorts.add(SortOptions.of(so -> so
                    .field(f -> f.field("uploadedAt").order(SortOrder.Desc))
            ));

            var response = elasticsearchClient.search(s -> s
                            .index(ImageDocument.IMAGES)
                            .query(q -> hasQuery
                                    ? q.multiMatch(m -> m.query(req.getQuery()).fields("text"))
                                    : q.matchAll(m -> m)
                            )
                            .sort(sorts)
                            .from(req.getPage() * req.getSize())
                            .size(req.getSize()),
                    ImageDocument.class
            );

            long totalElements = response.hits().total() != null
                    ? response.hits().total().value() : 0L;
            int totalPages = (int) Math.ceil((double) totalElements / req.getSize());

            var results = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(doc -> SearchResult.builder()
                            .id(doc.getId())
                            .text(doc.getText())
                            .imageUrl(doc.getImageUrl())
                            .uploadedAt(doc.getUploadedAt())
                            .build()
                    )
                    .toList();

            return ApiResponse.setSuccess(PagedResult.<SearchResult>builder()
                    .contents(results)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .currentPage(req.getPage())
                    .size(req.getSize())
                    .build());

        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return ApiResponse.setResponse(null, "Search failed", 500);
        }
    }

    private boolean isDuplicate(String text) {
        try {
            var normalized = normalize(text);


            var exactResponse = elasticsearchClient.search(s -> s
                            .index(ImageDocument.IMAGES)
                            .query(q -> q
                                    .matchPhrase(m -> m
                                            .field("text")
                                            .query(normalized)
                                    )
                            )
                            .size(1),
                    ImageDocument.class
            );

            if (!exactResponse.hits().hits().isEmpty()) {
                log.info("Exact duplicate found, rejecting upload");
                return true;
            }


            var fuzzyResponse = elasticsearchClient.search(s -> s
                            .index(ImageDocument.IMAGES)
                            .query(q -> q
                                    .moreLikeThis(mlt -> mlt
                                            .fields("text")
                                            .like(l -> l.text(normalized))
                                            .minTermFreq(1)
                                            .maxQueryTerms(10)
                                            .minDocFreq(1) // important: lower this for small index
                                    )
                            )
                            .size(5),
                    ImageDocument.class
            );

            return fuzzyResponse.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .anyMatch(doc -> similarity(normalized, normalize(doc.getText())) > DUPLICATE_THRESHOLD);

        } catch (Exception e) {
            log.warn("Duplicate check failed, proceeding with upload: {}", e.getMessage());
            return false;
        }
    }

    private String normalize(String text) {
        return text.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private double similarity(String a, String b) {
        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) distance / maxLen;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }
}
