package com.manadoksli.service;

import com.manadoksli.dto.ApiResponse;
import com.manadoksli.dto.PagedResult;
import com.manadoksli.dto.SearchReq;
import com.manadoksli.dto.SearchResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IImageService {

    ApiResponse<String> upload(MultipartFile file) throws Exception;
    ApiResponse<PagedResult<SearchResult>> search(SearchReq req) throws IOException;
}
