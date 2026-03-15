package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoAsyncRepository;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@Typed(DynamoDbTodoAsyncRepository.class)
@ApplicationScoped
public class DynamoDbTodoAsyncRepository implements TodoAsyncRepository {

    private static final String TODO_ID_COUNTER = "todo_id";

    private final DynamoDbAsyncClient client;
    private final String todoTable;
    private final String counterTable;

    @Inject
    public DynamoDbTodoAsyncRepository(
            DynamoDbAsyncClient client,
            @ConfigProperty(name = "app.dynamodb.table.todo", defaultValue = "todo_tasks")
                    String todoTable,
            @ConfigProperty(name = "app.dynamodb.table.counter", defaultValue = "app_counters")
                    String counterTable) {
        this.client = client;
        this.todoTable = todoTable;
        this.counterTable = counterTable;
    }

    @Override
    public Uni<List<TodoTask>> findAll() {
        return scanAll(null, new ArrayList<>())
                .map(
                        tasks ->
                                tasks.stream()
                                        .sorted(Comparator.comparingInt(TodoTask::id))
                                        .collect(Collectors.toList()));
    }

    private Uni<List<TodoTask>> scanAll(
            Map<String, AttributeValue> lastKey, List<TodoTask> accumulated) {
        var builder = ScanRequest.builder().tableName(todoTable);
        if (lastKey != null && !lastKey.isEmpty()) {
            builder.exclusiveStartKey(lastKey);
        }
        return Uni.createFrom()
                .completionStage(client.scan(builder.build()))
                .flatMap(
                        response -> {
                            response.items().stream()
                                    .map(this::toTodoTask)
                                    .forEach(accumulated::add);
                            if (response.lastEvaluatedKey() != null
                                    && !response.lastEvaluatedKey().isEmpty()) {
                                return scanAll(response.lastEvaluatedKey(), accumulated);
                            }
                            return Uni.createFrom().item(accumulated);
                        });
    }

    @Override
    public Uni<TodoTask> findById(String id) {
        return Uni.createFrom()
                .completionStage(
                        client.getItem(
                                GetItemRequest.builder()
                                        .tableName(todoTable)
                                        .key(Map.of("id", AttributeValue.fromN(id)))
                                        .build()))
                .map(
                        response -> {
                            var item = response.item();
                            if (item == null || item.isEmpty()) {
                                return null;
                            }
                            return toTodoTask(item);
                        });
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
                                    .completionStage(
                                            client.putItem(
                                                    PutItemRequest.builder()
                                                            .tableName(todoTable)
                                                            .item(toItem(newTask))
                                                            .build()))
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
                                    .completionStage(
                                            client.putItem(
                                                    PutItemRequest.builder()
                                                            .tableName(todoTable)
                                                            .item(toItem(updated))
                                                            .build()))
                                    .map(ignored -> updated);
                        });
    }

    @Override
    public Uni<Boolean> delete(Integer id) {
        return Uni.createFrom()
                .completionStage(
                        client.deleteItem(
                                DeleteItemRequest.builder()
                                        .tableName(todoTable)
                                        .key(Map.of("id", AttributeValue.fromN(id.toString())))
                                        .returnValues(ReturnValue.ALL_OLD)
                                        .build()))
                .map(response -> response.attributes() != null && !response.attributes().isEmpty());
    }

    private Uni<Integer> nextId() {
        return Uni.createFrom()
                .completionStage(
                        client.updateItem(
                                UpdateItemRequest.builder()
                                        .tableName(counterTable)
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

    private Map<String, AttributeValue> toItem(TodoTask task) {
        return Map.of(
                "id", AttributeValue.fromN(task.id().toString()),
                "title", AttributeValue.fromS(task.title()),
                "completed", AttributeValue.fromBool(task.isCompleted()),
                "createdAt", AttributeValue.fromN(String.valueOf(task.createdAt())),
                "updatedAt", AttributeValue.fromN(String.valueOf(task.updatedAt())));
    }

    private TodoTask toTodoTask(Map<String, AttributeValue> item) {
        return new TodoTask(
                Integer.parseInt(item.get("id").n()),
                item.get("title").s(),
                item.get("completed").bool(),
                Long.parseLong(item.get("createdAt").n()),
                Long.parseLong(item.get("updatedAt").n()));
    }
}
