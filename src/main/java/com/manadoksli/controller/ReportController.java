package com.manadoksli.controller;

import com.manadoksli.aspect.annotation.LogRequestResponse;
import com.manadoksli.constant.ApiVersionConstant;
import com.manadoksli.dto.ApiResponse;
import com.manadoksli.dto.ReportReq;
import com.manadoksli.service.IReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/images")
@RequiredArgsConstructor
@LogRequestResponse
public class ReportController {

    private final IReportService reportService;

    @PostMapping(value = "/report", version = ApiVersionConstant.V1,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> report(@Valid @RequestBody ReportReq req) {
        return reportService.report(req).toResponseEntity();
    }
}
