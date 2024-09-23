package io.github.yuokada.practice;

import static io.smallrye.mutiny.Uni.join;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class TodoAsyncService {

    private static final String ID_KEY = TodoServiceConstants.ID_KEY;

    private final ReactiveValueCommands<String, TodoTask> todoCommands;
    private final ReactiveKeyCommands<String> keyCommands;

    @Inject
    public TodoAsyncService(ReactiveRedisDataSource ds) {
        this.todoCommands = ds.value(TodoTask.class);
        this.keyCommands = ds.key();
    }

    public List<TodoTask> tasks() {
        List<String> keys = keyCommands.keys("*")
            .subscribeAsCompletionStage().join();
        return keys.stream()
            .filter(k -> k.matches("^\\d+$"))
            .map(this::syncTask)
            .collect(Collectors.toList());
    }

    private Uni<TodoTask> task(String id) {
        return todoCommands.get(id);
    }

    private TodoTask syncTask(String id) {
        return todoCommands.get(id).subscribeAsCompletionStage().join();
    }

    public Uni<TodoTask> task(Integer id) {
        return todoCommands.get(id.toString());
    }

    public TodoTask create(TodoTask task) {
        try {
            Integer nextId = nextId();
            TodoTask newTask = new TodoTask(nextId, task.title(), task.isCompleted());
            todoCommands.set(nextId.toString(), newTask);
            join();
            return newTask;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer nextId() throws ExecutionException, InterruptedException {
        // NOTE: This is not a safe way to generate unique IDs
        Long result = todoCommands.incrby(ID_KEY, 2).subscribeAsCompletionStage().get();
        return result.intValue();
    }

    public TodoTask update(Integer id, TodoTask task) {
        TodoTask currentTask = syncTask(id.toString());
        if (currentTask == null) {
            return null;
        }
        TodoTask updatedTask = new TodoTask(id, task.title(), task.isCompleted());
        todoCommands.set(id.toString(), updatedTask);
        return updatedTask;
    }

    public boolean delete(Integer id) throws ExecutionException, InterruptedException {
        Uni<Boolean> uniResult = keyCommands.del(id.toString()).map(l -> l == 1L);
        return uniResult.subscribeAsCompletionStage().get();
    }
}
