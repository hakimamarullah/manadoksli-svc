package com.manadoksli.service.impl;

import com.manadoksli.dto.ApiResponse;
import com.manadoksli.dto.ReportReq;
import com.manadoksli.model.ImageDocument;
import com.manadoksli.model.ReportDocument;
import com.manadoksli.repository.ImageDocumentRepository;
import com.manadoksli.repository.ReportDocumentRepository;
import com.manadoksli.service.IReportService;
import com.manadoksli.service.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSvc implements IReportService {

    private final ReportDocumentRepository reportRepository;
    private final ImageDocumentRepository imageRepository;
    private final IStorageService storageService;

    private static final long AUTO_DELETE_THRESHOLD = 3;

    @Override
    public ApiResponse<Void> report(ReportReq req) {
        var imageOpt = imageRepository.findById(req.getImageId());
        if (imageOpt.isEmpty()) {
            return ApiResponse.setResponse(null, "Image not found", 404);
        }


        var report = ReportDocument.builder()
                .id(UUID.randomUUID().toString())
                .imageId(req.getImageId())
                .reason(req.getReason())
                .reportedAt(OffsetDateTime.now())
                .build();

        reportRepository.save(report);
        log.info("Image {} reported for: {}", req.getImageId(), req.getReason());


        CompletableFuture.runAsync(() -> processReportAsync(req.getImageId(), imageOpt.get()));

        return ApiResponse.setDefaultSuccess();
    }


    public void processReportAsync(String imageId, ImageDocument image) {
        try {
            long reportCount = reportRepository.countByImageId(imageId);
            log.info("Image {} has {} report(s)", imageId, reportCount);

            if (reportCount >= AUTO_DELETE_THRESHOLD) {
                log.warn("Image {} reached threshold ({}), deleting...", imageId, reportCount);

                // Delete file from RustFS
                deleteImageFile(image.getImageUrl());

                // Delete from Elasticsearch
                imageRepository.deleteById(imageId);
                log.info("Image {} auto-deleted after {} reports", imageId, reportCount);
            }
        } catch (Exception e) {
            log.error("Async report processing failed for image {}: {}", imageId, e.getMessage(), e);
        }
    }

    private void deleteImageFile(String imageUrl) {
        try {
            storageService.delete(imageUrl);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage: {}", e.getMessage());
        }
    }
}