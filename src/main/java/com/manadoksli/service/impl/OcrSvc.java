package com.manadoksli.service.impl;

import com.manadoksli.dto.OcrResult;
import com.manadoksli.service.IOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@RegisterReflectionForBinding({
        OcrResult.class,
        MultiValueMap.class,
        LinkedMultiValueMap.class,
})
public class OcrSvc implements IOcrService {

    private final WebClient webClient;

    @Value("${ocr.api-key}")
    private String apiKey;

    @Value("${ocr.url:https://api.ocr.space/parse/image}")
    private String ocrUrl;

    @Override
    public String extractText(MultipartFile file) {
        try {
            var base64 = Base64.getEncoder().encodeToString(file.getBytes());

            var body = constructRequestBody(file, base64);

            var response = Optional.ofNullable(webClient.post()
                            .uri(ocrUrl)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromFormData(body))
                            .retrieve()
                            .bodyToMono(OcrResult.class)
                            .subscribeOn(Schedulers.boundedElastic())
                            .block())
                    .orElseThrow(() -> new RuntimeException("Empty Response"));

            log.debug("OCR raw response: {}", response);


            if (Boolean.TRUE.equals(response.getIsErrorOnProcessing())) {
                log.error("OCR API error: {}", response.getParsedResults().getFirst().getErrorMessage());
                return "";
            }

            var parsedResults = response.getParsedResults();
            if (!parsedResults.isEmpty()) {
                return parsedResults.getFirst().getParsedText().trim();
            }

        } catch (Exception e) {
            log.error("OCR extraction failed: {}", e.getMessage(), e);
        }
        return "";
    }

    private @NonNull MultiValueMap<String, String> constructRequestBody(MultipartFile file, String base64) {
        var contentType = Optional.ofNullable(file.getContentType())
                .orElse("image/jpeg");
        var base64Image = "data:" + contentType + ";base64," + base64;

        var body = new LinkedMultiValueMap<String, String>();
        body.add("apikey", apiKey);
        body.add("base64Image", base64Image);
        body.add("isOverlayRequired", "false");
        body.add("detectOrientation", "true");
        body.add("scale", "true");
        body.add("OCREngine", "2");
        return body;
    }

}
