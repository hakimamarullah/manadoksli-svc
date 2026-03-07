package com.manadoksli.config;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Configuration
@Slf4j
public class S3Config {

    @Bean
    SdkHttpClient apacheHttpClient(@Value("${apache.client.connect-timeout:30s}") Duration connectTimeout,
                                   @Value("${apache.client.socket-timeout:60s}") Duration socketTimeout,
                                   @Value("${apache.client.max-conn:100}") Integer maxConn) throws NoSuchAlgorithmException, KeyManagementException {
        log.warn("SDKHttpClient is using insecure SSL context");
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // Skip client certificate validation
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // Skip server certificate validation
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        X509TrustManager trustManager = (X509TrustManager) trustAllCerts[0];
        return ApacheHttpClient.builder()
                .connectionTimeout(connectTimeout)
                .socketTimeout(socketTimeout)
                .maxConnections(maxConn)
                .tlsTrustManagersProvider(() -> new TrustManager[]{trustManager})
                .build();
    }

    @Bean
    S3Client s3Client(RustFsProperties properties, SdkHttpClient client) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );

        return S3Client.builder()
                .region(properties.getRegion())
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .httpClient(client)
                .build();
    }

    @Bean
    S3Presigner s3Presigner(RustFsProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(properties.getRegion())
                .credentialsProvider(StaticCredentialsProvider.create(credentials));


        if (StringUtils.isNotBlank(properties.getPublicUrl())) {
            builder.endpointOverride(URI.create(properties.getPublicUrl()));
        }

        builder.serviceConfiguration(
                S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build()
        );

        return builder.build();
    }

}
