package com.manadoksli.service;

import org.springframework.web.multipart.MultipartFile;

public interface IStorageService {

    void delete(String fileUrl);

    String upload(MultipartFile file);
}
