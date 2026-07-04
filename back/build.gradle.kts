plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.allochub"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.hibernate.orm:hibernate-community-dialects")
    // 로컬 개발(default profile) 전용. 테스트/운영은 PostgreSQL.
    runtimeOnly("org.xerial:sqlite-jdbc:3.47.2.0")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // application-test.yml → PostgreSQL (운영과 동일)
    systemProperty("spring.profiles.active", "test")
}
