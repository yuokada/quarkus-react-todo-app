# Future Work: DynamoDB Local Integration Tests via Testcontainers

## Overview

Currently, tests run against Redis (`%test.app.repository.type=redis`) because no DynamoDB Local
container is available in CI. This document describes how to add proper DynamoDB integration tests
using Testcontainers.

## Implementation Plan

### 1. Add Testcontainers dependency

Add to `pom.xml` (test scope):

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

Testcontainers is already pulled in transitively, but an explicit dependency makes the intent clear.

### 2. Create `DynamoDbLocalResource`

`src/test/java/io/github/yuokada/practice/DynamoDbLocalResource.java`

```java
package io.github.yuokada.practice;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.Map;

public class DynamoDbLocalResource implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGE = "amazon/dynamodb-local:2.4.0";

    @SuppressWarnings("resource")
    private final GenericContainer<?> container =
            new GenericContainer<>(IMAGE).withExposedPorts(8000);

    @Override
    public Map<String, String> start() {
        container.start();
        String endpoint = "http://localhost:" + container.getMappedPort(8000);
        createTables(endpoint);
        return Map.of(
                "app.repository.type", "dynamodb",
                "app.dynamodb.endpoint-override", endpoint);
    }

    @Override
    public void stop() {
        container.stop();
    }

    private void createTables(String endpoint) {
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.AP_NORTHEAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build()) {

            // todo_tasks table
            client.createTable(CreateTableRequest.builder()
                    .tableName("todo_tasks")
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id")
                            .attributeType(ScalarAttributeType.N)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("id")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());

            // app_counters table
            client.createTable(CreateTableRequest.builder()
                    .tableName("app_counters")
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("key")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("key")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        }
    }
}
```

### 3. Annotate test classes

Add `@QuarkusTestResource` to each test class that should run against DynamoDB:

```java
@QuarkusTest
@QuarkusTestResource(DynamoDbLocalResource.class)
class TodoResourceTest { ... }
```

Or create a dedicated test class (e.g. `TodoDynamoDbResourceTest`) that mirrors the Redis tests
but runs against DynamoDB, keeping the Redis tests as-is for regression coverage of both backends.

### 4. Remove the test profile override (or keep both)

Once `DynamoDbLocalResource` injects `app.repository.type=dynamodb` at runtime, the
`%test.app.repository.type=redis` override in `application.properties` is no longer needed
for DynamoDB-specific test classes. Keep it as the default so that tests without
`@QuarkusTestResource(DynamoDbLocalResource.class)` still use Redis.

## Benefits

- Tests cover the actual DynamoDB repository implementations
- Container lifecycle is managed automatically (no manual `docker compose up` needed)
- Works in CI without changes to the GitHub Actions workflow (Docker socket is available)
- Port is dynamically assigned — no conflicts with local DynamoDB Local on port 8000

## Estimated Effort

Small: ~1–2 hours. The `DynamoDbLocalResource` above is mostly self-contained; the main
work is deciding whether to annotate existing tests or create separate DynamoDB-specific ones.
