package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.lookup.LookupIfProperty;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoRepository;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@Typed(DynamoDbTodoRepository.class)
@ApplicationScoped
public class DynamoDbTodoRepository implements TodoRepository {

    private static final String TODO_ID_COUNTER = "todo_id";

    private final DynamoDbTable<TodoTaskItem> todoTable;
    private final DynamoDbClient client;
    private final String counterTableName;

    @Inject
    public DynamoDbTodoRepository(
            DynamoDbClient client,
            DynamoDbEnhancedClient enhancedClient,
            @ConfigProperty(name = "app.dynamodb.table.todo", defaultValue = "todo_tasks")
                    String todoTableName,
            @ConfigProperty(name = "app.dynamodb.table.counter", defaultValue = "app_counters")
                    String counterTableName) {
        this.client = client;
        this.counterTableName = counterTableName;
        this.todoTable =
                enhancedClient.table(todoTableName, TableSchema.fromBean(TodoTaskItem.class));
    }

    @Override
    public List<TodoTask> findAll() {
        return todoTable.scan().items().stream()
                .map(TodoTaskItem::toTask)
                .sorted(Comparator.comparingInt(TodoTask::id))
                .collect(Collectors.toList());
    }

    @Override
    public TodoTask findById(Integer id) {
        TodoTaskItem item = todoTable.getItem(Key.builder().partitionValue(id).build());
        return item == null ? null : item.toTask();
    }

    @Override
    public TodoTask create(TodoTask task) {
        Integer nextId = nextId();
        long now = System.currentTimeMillis();
        TodoTask newTask = new TodoTask(nextId, task.title(), task.isCompleted(), now, now);
        todoTable.putItem(TodoTaskItem.from(newTask));
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
        todoTable.putItem(TodoTaskItem.from(updated));
        return updated;
    }

    @Override
    public boolean delete(Integer id) {
        TodoTaskItem deleted = todoTable.deleteItem(Key.builder().partitionValue(id).build());
        return deleted != null;
    }

    private Integer nextId() {
        var attrs =
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
                                        .build())
                        .attributes();
        return Integer.parseInt(attrs.get("value").n());
    }
}
