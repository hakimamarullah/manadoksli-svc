package com.manadoksli.service;

import org.springframework.web.multipart.MultipartFile;

public interface IOcrService {

    String extractText(MultipartFile file);
}
