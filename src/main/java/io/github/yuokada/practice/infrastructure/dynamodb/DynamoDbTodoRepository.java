package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.lookup.LookupIfProperty;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoRepository;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@ApplicationScoped
public class DynamoDbTodoRepository implements TodoRepository {

    private static final String TODO_ID_COUNTER = "todo_id";

    @Inject DynamoDbClient client;

    @ConfigProperty(name = "app.dynamodb.table.todo", defaultValue = "todo_tasks")
    String todoTable;

    @ConfigProperty(name = "app.dynamodb.table.counter", defaultValue = "app_counters")
    String counterTable;

    @Override
    public List<TodoTask> findAll() {
        return client.scan(ScanRequest.builder().tableName(todoTable).build()).items().stream()
                .map(this::toTodoTask)
                .sorted(Comparator.comparingInt(TodoTask::id))
                .collect(Collectors.toList());
    }

    @Override
    public TodoTask findById(Integer id) {
        var item =
                client.getItem(
                                GetItemRequest.builder()
                                        .tableName(todoTable)
                                        .key(Map.of("id", AttributeValue.fromN(id.toString())))
                                        .build())
                        .item();
        if (item == null || item.isEmpty()) {
            return null;
        }
        return toTodoTask(item);
    }

    @Override
    public TodoTask create(TodoTask task) {
        Integer nextId = nextId();
        long now = System.currentTimeMillis();
        TodoTask newTask = new TodoTask(nextId, task.title(), task.isCompleted(), now, now);
        client.putItem(PutItemRequest.builder().tableName(todoTable).item(toItem(newTask)).build());
        return newTask;
    }

    @Override
    public TodoTask update(Integer id, TodoTask task) {
        TodoTask current = findById(id);
        if (current == null) {
            return null;
        }
        TodoTask updated =
                new TodoTask(
                        id,
                        task.title(),
                        task.isCompleted(),
                        current.createdAt(),
                        System.currentTimeMillis());
        client.putItem(PutItemRequest.builder().tableName(todoTable).item(toItem(updated)).build());
        return updated;
    }

    @Override
    public boolean delete(Integer id) {
        var deleted =
                client.deleteItem(
                                DeleteItemRequest.builder()
                                        .tableName(todoTable)
                                        .key(Map.of("id", AttributeValue.fromN(id.toString())))
                                        .returnValues(ReturnValue.ALL_OLD)
                                        .build())
                        .attributes();
        return deleted != null && !deleted.isEmpty();
    }

    private Integer nextId() {
        var attrs =
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
                                        .build())
                        .attributes();
        return Integer.parseInt(attrs.get("value").n());
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
