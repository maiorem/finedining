plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.finediningtheater"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // 일반 회원(카카오) 로그인. 관리자는 이 클라이언트와 무관한 자체 아이디·비밀번호 흐름이다
    // (CLAUDE.md §3.1·§7.4).
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.flywaydb:flyway-mysql")

    // 관리자 세션용 자체 JWT 발급 (CLAUDE.md §7.4) — OAuth2 리소스서버 전체를 끌어오지 않는다.
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // 이미지 업로드 presign + 파생본 저장 (CLAUDE.md §7.5). 로컬은 MinIO, 배포 시 환경변수만
    // 바꾸면 실제 AWS S3로 전환된다 — 엔드포인트 오버라이드 하나로 같은 코드가 양쪽에 다 붙는다.
    // S3Presigner는 별도 아티팩트가 아니라 s3 모듈 안에 포함되어 있다.
    implementation(platform("software.amazon.awssdk:bom:2.29.15"))
    implementation("software.amazon.awssdk:s3")

    runtimeOnly("com.mysql:mysql-connector-j")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
