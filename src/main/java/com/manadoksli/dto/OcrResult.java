package com.manadoksli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrResult {

    @JsonProperty("ParsedResults")
    private List<ParsedResult> parsedResults;

    @JsonProperty("IsErroredOnProcessing")
    private Boolean isErrorOnProcessing = Boolean.TRUE;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParsedResult {

        @JsonProperty("ParsedText")
        private String parsedText;

        @JsonProperty("ErrorMessage")
        private String errorMessage;

        @JsonProperty("ErrorDetails")
        private String errorDetails;

    }


}
