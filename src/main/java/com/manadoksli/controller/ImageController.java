package com.manadoksli.controller;

import com.manadoksli.aspect.annotation.LogRequestResponse;
import com.manadoksli.constant.ApiVersionConstant;
import com.manadoksli.dto.ApiResponse;
import com.manadoksli.dto.PagedResult;
import com.manadoksli.dto.SearchReq;
import com.manadoksli.dto.SearchResult;
import com.manadoksli.service.IImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@LogRequestResponse
public class ImageController {

    private final IImageService imageService;

    @PostMapping(value = "/upload", version = ApiVersionConstant.V1,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SneakyThrows
    public ResponseEntity<ApiResponse<String>> upload(@RequestParam("file") MultipartFile file) {
        return imageService.upload(file).toResponseEntity();
    }

    @GetMapping(value = "/search", version = ApiVersionConstant.V1,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SneakyThrows
    public ResponseEntity<ApiResponse<PagedResult<SearchResult>>> search(@Valid SearchReq req) {
        return imageService.search(req).toResponseEntity();
    }
}
