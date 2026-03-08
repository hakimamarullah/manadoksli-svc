package com.manadoksli.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IStorageService {

    void delete(String fileUrl);

    String upload(MultipartFile file) throws IOException;
}
