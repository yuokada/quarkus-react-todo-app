package io.github.yuokada.practice;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.testcontainers.containers.GenericContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

public class DynamoDbLocalResource implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGE = "amazon/dynamodb-local:3.3.0";
    private static final String TABLE_TODO = "todo_tasks";
    private static final String TABLE_COUNTER = "app_counters";

    @SuppressWarnings("resource")
    private final GenericContainer<?> container =
            new GenericContainer<>(IMAGE).withExposedPorts(8000);

    @Override
    public Map<String, String> start() {
        // Delegate to ForkJoinPool.commonPool where Testcontainers' Docker detection
        // works reliably (same pool used by Quarkus DevServices). The Quarkus test-resource
        // pool thread has a different classloader context that prevents Docker detection.
        String endpoint =
                CompletableFuture.supplyAsync(
                                () -> {
                                    container.start();
                                    return "http://localhost:" + container.getMappedPort(8000);
                                })
                        .thenApply(
                                ep -> {
                                    try {
                                        createTables(ep);
                                    } catch (Exception e) {
                                        container.stop();
                                        throw e;
                                    }
                                    return ep;
                                })
                        .join();

        return Map.of(
                "app.repository.type", "dynamodb",
                "app.dynamodb.endpoint-override", endpoint,
                "quarkus.redis.devservices.enabled", "false");
    }

    @Override
    public void stop() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    private void createTables(String ep) {
        try (DynamoDbClient client =
                DynamoDbClient.builder()
                        .endpointOverride(URI.create(ep))
                        .region(Region.AP_NORTHEAST_1)
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create("dummy", "dummy")))
                        .build()) {

            try (DynamoDbWaiter waiter = DynamoDbWaiter.builder().client(client).build()) {
                // todo_tasks table — partition key: id (Number)
                client.createTable(
                        CreateTableRequest.builder()
                                .tableName(TABLE_TODO)
                                .attributeDefinitions(
                                        AttributeDefinition.builder()
                                                .attributeName("id")
                                                .attributeType(ScalarAttributeType.N)
                                                .build())
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("id")
                                                .keyType(KeyType.HASH)
                                                .build())
                                .billingMode(BillingMode.PAY_PER_REQUEST)
                                .build());
                waiter.waitUntilTableExists(
                        DescribeTableRequest.builder().tableName(TABLE_TODO).build());

                // app_counters table — partition key: counterName (String)
                client.createTable(
                        CreateTableRequest.builder()
                                .tableName(TABLE_COUNTER)
                                .attributeDefinitions(
                                        AttributeDefinition.builder()
                                                .attributeName("counterName")
                                                .attributeType(ScalarAttributeType.S)
                                                .build())
                                .keySchema(
                                        KeySchemaElement.builder()
                                                .attributeName("counterName")
                                                .keyType(KeyType.HASH)
                                                .build())
                                .billingMode(BillingMode.PAY_PER_REQUEST)
                                .build());
                waiter.waitUntilTableExists(
                        DescribeTableRequest.builder().tableName(TABLE_COUNTER).build());
            }
        }
    }
}
