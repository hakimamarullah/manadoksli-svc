package com.manadoksli.service.impl;

import com.manadoksli.dto.OcrResult;
import com.manadoksli.exceptions.ApiException;
import com.manadoksli.service.IOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
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

    private final RestClient restClient;

    @Value("${ocr.api-key}")
    private String apiKey;

    @Value("${ocr.url:https://api.ocr.space/parse/image}")
    private String ocrUrl;

    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024L; // 1MB

    @Override
    public String extractText(MultipartFile file) throws IOException {

        var imageStream = prepareImage(file);
        var base64 = encodeToBase64Stream(imageStream);
        var body = constructRequestBody(file, base64);

        var response = restClient.post()
                .uri(ocrUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ApiException("OCR API HTTP error: " + res.getStatusCode());
                })
                .toEntity(OcrResult.class);

        var result = Objects.requireNonNull(response.getBody(), "Empty OCR response body");

        log.debug("OCR raw response: {}", result);

        if (Boolean.TRUE.equals(result.getIsErrorOnProcessing())) {
            log.error("OCR API error: {}", result.getParsedResults().getFirst().getErrorMessage());
            return "";
        }

        var parsedResults = result.getParsedResults();
        if (!parsedResults.isEmpty()) {
            return parsedResults.getFirst().getParsedText().trim();
        }


        return "";
    }

    /**
     * Returns original stream if under 1MB, otherwise compresses to JPEG.
     * Uses InputStream throughout — never loads full bytes unless compression needed.
     */
    private InputStream prepareImage(MultipartFile file) {
        try {
            if (file.getSize() <= MAX_FILE_SIZE_BYTES) {
                return file.getInputStream();
            }

            log.info("Image too large ({}KB), compressing...", file.getSize() / 1024);

            var image = ImageIO.read(file.getInputStream());
            if (image == null) return file.getInputStream();

            var quality = 0.7f;
            byte[] compressed;
            do {
                compressed = compressJpeg(image, quality);
                log.debug("Compressed at quality {}: {}KB", quality, compressed.length / 1024);
                quality -= 0.1f;
            } while (compressed.length > MAX_FILE_SIZE_BYTES && quality > 0.2f);

            log.info("Final size after compression: {}KB", compressed.length / 1024);
            return new ByteArrayInputStream(compressed);
        } catch (Exception e) {
            throw new ApiException("error.system-unavailable", e);
        }
    }

    private String encodeToBase64Stream(InputStream in) throws IOException {
        var out = new ByteArrayOutputStream(Math.max(1024, (in.available() * 4 / 3) + 4));
        try (in; var b64 = Base64.getEncoder().wrap(out)) {
            in.transferTo(b64);
        }
        return out.toString(StandardCharsets.ISO_8859_1);
    }

    private byte[] compressJpeg(BufferedImage image, float quality) throws IOException {
        var out = new ByteArrayOutputStream();
        var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        var param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        return out.toByteArray();
    }

    private @NonNull MultiValueMap<String, String> constructRequestBody(MultipartFile file, String base64) {
        var contentType = Optional.ofNullable(file.getContentType()).orElse("image/jpeg");
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
