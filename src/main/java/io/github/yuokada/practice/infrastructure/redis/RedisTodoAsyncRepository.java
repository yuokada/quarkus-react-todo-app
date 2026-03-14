package io.github.yuokada.practice.infrastructure.redis;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoAsyncRepository;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RedisTodoAsyncRepository implements TodoAsyncRepository {

    private static final String ID_KEY = RedisConstants.TODO_ID_KEY;

    private final ReactiveValueCommands<String, TodoTask> todoCommands;
    private final ReactiveKeyCommands<String> keyCommands;
    private final Logger logger;

    @Inject
    public RedisTodoAsyncRepository(ReactiveRedisDataSource ds, Logger logger) {
        this.todoCommands = ds.value(TodoTask.class);
        this.keyCommands = ds.key();
        this.logger = logger;
    }

    @Override
    public Uni<TodoTask> findById(String id) {
        return todoCommands.get(id).onFailure().retry().atMost(5);
    }

    @Override
    public Uni<List<TodoTask>> findAll() {
        return keyCommands
                .keys("*")
                .onItem()
                .transform(
                        keys ->
                                keys.stream()
                                        .filter(k -> k.matches("^\\d+$"))
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

    @Override
    public Uni<TodoTask> create(TodoTask task) {
        return nextId()
                .onItem()
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

    @Override
    public Uni<TodoTask> update(Integer id, TodoTask task) {
        return findById(id.toString())
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

    @Override
    public Uni<Boolean> delete(Integer id) {
        return keyCommands.del(id.toString()).map(l -> l == 1L);
    }
}
