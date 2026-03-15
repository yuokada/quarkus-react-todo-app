package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import io.github.yuokada.practice.domain.repository.IncrementRepository;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@Typed(DynamoDbIncrementRepository.class)
@ApplicationScoped
public class DynamoDbIncrementRepository implements IncrementRepository {

    // Keys used internally by the application, excluded from keys()
    private static final Set<String> INTERNAL_KEYS = Set.of("todo_id");

    private final DynamoDbClient client;
    private final DynamoDbAsyncClient asyncClient;
    private final String counterTable;

    @Inject
    public DynamoDbIncrementRepository(
            DynamoDbClient client,
            DynamoDbAsyncClient asyncClient,
            @ConfigProperty(name = "app.dynamodb.table.counter", defaultValue = "app_counters")
                    String counterTable) {
        this.client = client;
        this.asyncClient = asyncClient;
        this.counterTable = counterTable;
    }

    @Override
    public long get(String key) {
        var item =
                client.getItem(
                                GetItemRequest.builder()
                                        .tableName(counterTable)
                                        .key(Map.of("counterName", AttributeValue.fromS(key)))
                                        .build())
                        .item();
        if (item == null || item.isEmpty() || !item.containsKey("value")) {
            return 0L;
        }
        return Long.parseLong(item.get("value").n());
    }

    @Override
    public void set(String key, long value) {
        client.putItem(
                PutItemRequest.builder()
                        .tableName(counterTable)
                        .item(
                                Map.of(
                                        "counterName", AttributeValue.fromS(key),
                                        "value", AttributeValue.fromN(String.valueOf(value))))
                        .build());
    }

    @Override
    public void increment(String key, long incrementBy) {
        client.updateItem(
                UpdateItemRequest.builder()
                        .tableName(counterTable)
                        .key(Map.of("counterName", AttributeValue.fromS(key)))
                        .updateExpression("ADD #val :incr")
                        .expressionAttributeNames(Map.of("#val", "value"))
                        .expressionAttributeValues(
                                Map.of(":incr", AttributeValue.fromN(String.valueOf(incrementBy))))
                        .returnValues(ReturnValue.UPDATED_NEW)
                        .build());
    }

    @Override
    public Uni<Void> delete(String key) {
        return Uni.createFrom()
                .completionStage(
                        asyncClient.deleteItem(
                                DeleteItemRequest.builder()
                                        .tableName(counterTable)
                                        .key(Map.of("counterName", AttributeValue.fromS(key)))
                                        .build()))
                .replaceWithVoid();
    }

    @Override
    public Uni<List<String>> keys() {
        return Uni.createFrom()
                .completionStage(
                        asyncClient.scan(ScanRequest.builder().tableName(counterTable).build()))
                .map(
                        response ->
                                response.items().stream()
                                        .map(item -> item.get("counterName").s())
                                        .filter(k -> !INTERNAL_KEYS.contains(k))
                                        .collect(Collectors.toList()));
    }
}
