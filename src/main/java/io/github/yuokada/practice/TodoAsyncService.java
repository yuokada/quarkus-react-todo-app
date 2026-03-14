package io.github.yuokada.practice;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TodoAsyncService {

    private static final String ID_KEY = TodoServiceConstants.ID_KEY;

    private final ReactiveValueCommands<String, TodoTask> todoCommands;
    private final ReactiveKeyCommands<String> keyCommands;
    private final Logger logger;

    @Inject
    public TodoAsyncService(ReactiveRedisDataSource ds, Logger logger) {
        this.todoCommands = ds.value(TodoTask.class);
        this.keyCommands = ds.key();
        this.logger = logger;
    }

    public Uni<TodoTask> asyncTask(String id) {
        // Get the task asynchronously without using the task() method
        return todoCommands.get(id).onFailure().retry().atMost(5);
    }

    public Uni<List<TodoTask>> tasks() {
        return keyCommands
                .keys("*")
                .onItem()
                .transform(
                        keys ->
                                keys.stream()
                                        .filter(k -> k.matches("^\\d+$")) // Filter keys that are
                                        // integers
                                        .collect(Collectors.toList()))
                .onItem()
                .invoke(keys -> logger.infof("Task IDs: %s", keys))
                .onItem()
                .transformToMulti(keys -> Multi.createFrom().items(keys.stream()))
                .onItem()
                .transformToUniAndConcatenate(todoCommands::get)
                .onItem()
                .invoke(task -> logger.infof("Task detail: %s", task))
                .collect()
                .asList()
                .onItem()
                .transform(
                        tasks ->
                                tasks.stream()
                                        .sorted(Comparator.comparingInt(TodoTask::id))
                                        .collect(Collectors.toList()));
    }

    public Uni<TodoTask> create(TodoTask task) {
        return nextId().onItem()
                .transform(
                        nextId -> {
                            logger.infof("Next ID: %d", nextId);
                            return new TodoTask(nextId, task.title(), task.isCompleted());
                        })
                .onItem()
                .transformToUni(
                        newTask ->
                                todoCommands
                                        .set(newTask.id().toString(), newTask)
                                        .replaceWith(newTask)
                                        .invoke(() -> logger.info("Task created")));
    }

    private Uni<Integer> nextId() {
        return todoCommands.incrby(ID_KEY, 2).map(Long::intValue);
    }

    public Uni<TodoTask> updateAsync(Integer id, TodoTask task) {
        return asyncTask(id.toString())
                .onItem()
                .transformToUni(
                        currentTask -> {
                            if (currentTask == null) {
                                return Uni.createFrom().nullItem();
                            }
                            TodoTask updatedTask =
                                    new TodoTask(id, task.title(), task.isCompleted());
                            return todoCommands
                                    .set(id.toString(), updatedTask)
                                    .onItem()
                                    .transform(v -> updatedTask);
                        });
    }

    public Uni<Boolean> delete(Integer id) {
        return keyCommands.del(id.toString()).map(l -> l == 1L);
    }
}
