package com.finediningtheater.media;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** S3(MinIO 호환) 접근을 감싼다. presign 발급과 파생본 저장·삭제가 전부 여기를 거친다 (CLAUDE.md §7.5). */
@Component
@RequiredArgsConstructor
public class MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaProperties properties;

    /**
     * 로컬 MinIO는 버킷을 미리 만들어두지 않아도 되게 앱이 직접 만든다. 실제 AWS 계정에서는
     * auto-create-bucket을 false로 두므로 이 메서드는 조용히 아무 일도 하지 않는다 (CLAUDE.md §13.7).
     * 프로덕션은 CloudFront가 오리진 접근 제어로 S3를 대신 읽어 공개하지만(§13.3), 로컬 개발에는
     * CloudFront가 없으므로 파생본을 브라우저에서 바로 보려면 버킷 자체에 공개 읽기 정책을 걸어야
     * 한다 — 이 정책도 auto-create-bucket(로컬 전용) 아래에서만 적용된다.
     *
     * <p>MinIO가 아직 안 떠 있어도(로컬에서 mysql만 띄우고 개발할 때, §14) 앱 전체가 죽으면 안
     * 된다 — 이미지 업로드만 그 시점까지 못 쓸 뿐 나머지 기능은 정상 동작해야 한다. 그래서 여기서
     * 발생하는 모든 예외를 삼키고 경고만 남긴다. {@code ApplicationReadyEvent} 리스너가 예외를
     * 던지면 Spring Boot가 그걸 부팅 실패로 취급해 애플리케이션 전체가 종료된다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucketExists() {
        if (!properties.autoCreateBucket()) {
            return;
        }
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
                } else {
                    throw e;
                }
            }
            s3Client.putBucketPolicy(
                    PutBucketPolicyRequest.builder().bucket(properties.bucket()).policy(publicReadPolicy()).build());
        } catch (Exception e) {
            log.warn(
                    "로컬 S3(MinIO)에 연결할 수 없어 버킷 준비를 건너뜁니다 — 이미지 업로드 기능만 영향을"
                            + " 받습니다. `docker compose -f docker-compose.yml -f docker-compose.local.yml up -d minio`로"
                            + " 띄우세요. ({})",
                    e.getMessage());
        }
    }

    private String publicReadPolicy() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """
                .formatted(properties.bucket());
    }

    public URL presignPut(String key, String contentType, Duration validity) {
        PutObjectRequest putRequest =
                PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(contentType).build();
        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder().signatureDuration(validity).putObjectRequest(putRequest).build();
        return s3Presigner.presignPutObject(presignRequest).url();
    }

    public HeadObjectResponse headObject(String key) {
        return s3Client.headObject(HeadObjectRequest.builder().bucket(properties.bucket()).key(key).build());
    }

    public byte[] getObjectBytes(String key) {
        return s3Client
                .getObjectAsBytes(GetObjectRequest.builder().bucket(properties.bucket()).key(key).build())
                .asByteArray();
    }

    public byte[] getObjectRange(String key, long start, long end) {
        return s3Client
                .getObjectAsBytes(
                        GetObjectRequest.builder()
                                .bucket(properties.bucket())
                                .key(key)
                                .range("bytes=" + start + "-" + end)
                                .build())
                .asByteArray();
    }

    public void putObject(String key, byte[] data, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(data));
    }

    public void deleteObjects(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        List<ObjectIdentifier> ids = keys.stream().map(key -> ObjectIdentifier.builder().key(key).build()).toList();
        s3Client.deleteObjects(
                DeleteObjectsRequest.builder()
                        .bucket(properties.bucket())
                        .delete(Delete.builder().objects(ids).build())
                        .build());
    }

    public String publicUrl(String key) {
        return properties.publicBaseUrl() + "/" + key;
    }
}
