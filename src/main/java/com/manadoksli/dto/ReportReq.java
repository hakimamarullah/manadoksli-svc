package com.manadoksli.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportReq {

    @NotBlank
    private String imageId;

    private String reason = "DUPLICATE";
}