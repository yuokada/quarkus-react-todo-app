package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import io.github.yuokada.practice.domain.repository.IncrementRepository;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@Typed(DynamoDbIncrementRepository.class)
@ApplicationScoped
public class DynamoDbIncrementRepository implements IncrementRepository {

    // Keys used internally by the application, excluded from keys()
    private static final Set<String> INTERNAL_KEYS = Set.of("todo_id");

    private final DynamoDbTable<CounterItem> counterTable;
    private final DynamoDbAsyncTable<CounterItem> asyncCounterTable;
    private final DynamoDbClient client;
    private final String counterTableName;

    @Inject
    public DynamoDbIncrementRepository(
            DynamoDbClient client,
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbEnhancedAsyncClient enhancedAsyncClient,
            @ConfigProperty(name = "app.dynamodb.table.counter", defaultValue = "app_counters")
                    String counterTableName) {
        this.client = client;
        this.counterTableName = counterTableName;
        this.counterTable = enhancedClient.table(counterTableName, CounterItem.TABLE_SCHEMA);
        this.asyncCounterTable =
                enhancedAsyncClient.table(counterTableName, CounterItem.TABLE_SCHEMA);
    }

    @Override
    public long get(String key) {
        CounterItem item = counterTable.getItem(Key.builder().partitionValue(key).build());
        return item == null || item.getValue() == null ? 0L : item.getValue();
    }

    @Override
    public void set(String key, long value) {
        CounterItem item = new CounterItem();
        item.setCounterName(key);
        item.setValue(value);
        counterTable.putItem(item);
    }

    @Override
    public void increment(String key, long incrementBy) {
        client.updateItem(
                UpdateItemRequest.builder()
                        .tableName(counterTableName)
                        .key(Map.of("counterName", AttributeValue.fromS(key)))
                        .updateExpression("ADD #val :incr")
                        .expressionAttributeNames(Map.of("#val", "value"))
                        .expressionAttributeValues(
                                Map.of(":incr", AttributeValue.fromN(String.valueOf(incrementBy))))
                        .build());
    }

    @Override
    public Uni<Void> delete(String key) {
        return Uni.createFrom()
                .completionStage(
                        asyncCounterTable.deleteItem(Key.builder().partitionValue(key).build()))
                .replaceWithVoid();
    }

    @Override
    public Uni<List<String>> keys() {
        return Uni.createFrom()
                .completionStage(collectToList(asyncCounterTable.scan().items()))
                .map(
                        items ->
                                items.stream()
                                        .map(CounterItem::getCounterName)
                                        .filter(k -> !INTERNAL_KEYS.contains(k))
                                        .collect(Collectors.toList()));
    }

    private static <T> CompletableFuture<List<T>> collectToList(Publisher<T> publisher) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        publisher.subscribe(
                new Subscriber<T>() {
                    private final List<T> items = new ArrayList<>();

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(T item) {
                        items.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        future.complete(items);
                    }
                });
        return future;
    }
}
