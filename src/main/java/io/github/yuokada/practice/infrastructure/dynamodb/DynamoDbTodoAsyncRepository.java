package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoAsyncRepository;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@Typed(DynamoDbTodoAsyncRepository.class)
@ApplicationScoped
public class DynamoDbTodoAsyncRepository implements TodoAsyncRepository {

    private static final String TODO_ID_COUNTER = "todo_id";

    private final DynamoDbAsyncTable<TodoTaskItem> todoTable;
    private final DynamoDbAsyncClient client;
    private final String counterTableName;

    @Inject
    public DynamoDbTodoAsyncRepository(
            DynamoDbAsyncClient client,
            DynamoDbEnhancedAsyncClient enhancedAsyncClient,
            @ConfigProperty(name = "app.dynamodb.table.todo", defaultValue = "todo_tasks")
                    String todoTableName,
            @ConfigProperty(name = "app.dynamodb.table.counter", defaultValue = "app_counters")
                    String counterTableName) {
        this.client = client;
        this.counterTableName = counterTableName;
        this.todoTable = enhancedAsyncClient.table(todoTableName, TodoTaskItem.TABLE_SCHEMA);
    }

    @Override
    public Uni<List<TodoTask>> findAll() {
        return Uni.createFrom()
                .completionStage(collectToList(todoTable.scan().items()))
                .map(
                        items ->
                                items.stream()
                                        .map(TodoTaskItem::toTask)
                                        .sorted(Comparator.comparingInt(TodoTask::id))
                                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<TodoTask> findById(String id) {
        return Uni.createFrom()
                .completionStage(
                        todoTable.getItem(
                                Key.builder().partitionValue(Integer.parseInt(id)).build()))
                .map(item -> item == null ? null : item.toTask());
    }

    @Override
    public Uni<TodoTask> create(TodoTask task) {
        return nextId().flatMap(
                        nextId -> {
                            long now = System.currentTimeMillis();
                            TodoTask newTask =
                                    new TodoTask(
                                            nextId, task.title(), task.isCompleted(), now, now);
                            return Uni.createFrom()
                                    .completionStage(todoTable.putItem(TodoTaskItem.from(newTask)))
                                    .map(ignored -> newTask);
                        });
    }

    @Override
    public Uni<TodoTask> update(Integer id, TodoTask task) {
        return findById(id.toString())
                .flatMap(
                        current -> {
                            if (current == null) {
                                return Uni.createFrom().nullItem();
                            }
                            TodoTask updated =
                                    new TodoTask(
                                            id,
                                            task.title(),
                                            task.isCompleted(),
                                            current.createdAt(),
                                            System.currentTimeMillis());
                            return Uni.createFrom()
                                    .completionStage(todoTable.putItem(TodoTaskItem.from(updated)))
                                    .map(ignored -> updated);
                        });
    }

    @Override
    public Uni<Boolean> delete(Integer id) {
        return Uni.createFrom()
                .completionStage(todoTable.deleteItem(Key.builder().partitionValue(id).build()))
                .map(deleted -> deleted != null);
    }

    private Uni<Integer> nextId() {
        return Uni.createFrom()
                .completionStage(
                        client.updateItem(
                                UpdateItemRequest.builder()
                                        .tableName(counterTableName)
                                        .key(
                                                Map.of(
                                                        "counterName",
                                                        AttributeValue.fromS(TODO_ID_COUNTER)))
                                        .updateExpression("ADD #val :incr")
                                        .expressionAttributeNames(Map.of("#val", "value"))
                                        .expressionAttributeValues(
                                                Map.of(":incr", AttributeValue.fromN("2")))
                                        .returnValues(ReturnValue.UPDATED_NEW)
                                        .build()))
                .map(response -> Integer.parseInt(response.attributes().get("value").n()));
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
