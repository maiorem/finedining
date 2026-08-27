package com.finediningtheater.media;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(MediaProperties.class)
@RequiredArgsConstructor
public class MediaConfig {

    private final MediaProperties properties;

    @Bean
    public S3Client s3Client() {
        var builder =
                S3Client.builder()
                        .region(Region.of(properties.region()))
                        .credentialsProvider(credentialsProvider())
                        .serviceConfiguration(
                                S3Configuration.builder().pathStyleAccessEnabled(properties.pathStyleAccess()).build());
        applyEndpointOverride(builder::endpointOverride);
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder =
                S3Presigner.builder()
                        .region(Region.of(properties.region()))
                        .credentialsProvider(credentialsProvider())
                        .serviceConfiguration(
                                S3Configuration.builder().pathStyleAccessEnabled(properties.pathStyleAccess()).build());
        applyEndpointOverride(builder::endpointOverride);
        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }

    private void applyEndpointOverride(java.util.function.Consumer<URI> setter) {
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            setter.accept(URI.create(properties.endpoint()));
        }
    }
}
