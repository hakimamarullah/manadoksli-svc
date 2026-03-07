package com.manadoksli.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;


@Component
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
@RegisterReflectionForBinding({
        Region.class
})
public class RustFsProperties {

    private String endpoint;

    private String accessKey;

    private String bucket;

    private String publicUrl;

    private String secretKey;

    private Region region;
}
