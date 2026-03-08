package com.manadoksli.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IOcrService {

    String extractText(MultipartFile file) throws IOException;
}
