package com.manadoksli.service;

import com.manadoksli.dto.ApiResponse;
import com.manadoksli.dto.ReportReq;

public interface IReportService {
    ApiResponse<Void> report(ReportReq req);
}