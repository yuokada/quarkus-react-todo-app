package io.github.yuokada.practice.infrastructure.redis;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoRepository;

@Typed(RedisTodoRepository.class)
@ApplicationScoped
public class RedisTodoRepository implements TodoRepository {

    private static final String ID_KEY = RedisConstants.TODO_ID_KEY;

    private final ValueCommands<String, TodoTask> todoCommands;
    private final KeyCommands<String> keyCommands;

    @Inject
    public RedisTodoRepository(RedisDataSource ds) {
        this.todoCommands = ds.value(TodoTask.class);
        this.keyCommands = ds.key();
    }

    @Override
    public List<TodoTask> findAll() {
        List<String> keys = keyCommands.keys("*");
        return keys.stream()
                .filter(k -> k.matches("^\\d+$"))
                .map(this::findByKey)
                .collect(Collectors.toList());
    }

    private TodoTask findByKey(String id) {
        return todoCommands.get(id);
    }

    @Override
    public TodoTask findById(Integer id) {
        return todoCommands.get(id.toString());
    }

    @Override
    public TodoTask create(TodoTask task) {
        Integer nextId = nextId();
        long now = System.currentTimeMillis();
        TodoTask newTask = new TodoTask(nextId, task.title(), task.isCompleted(), now, now);
        todoCommands.set(nextId.toString(), newTask);
        return newTask;
    }

    private Integer nextId() {
        // NOTE: This is not a safe way to generate unique IDs
        return (int) todoCommands.incrby(ID_KEY, 2);
    }

    @Override
    public TodoTask update(Integer id, TodoTask task) {
        TodoTask currentTask = todoCommands.get(id.toString());
        if (currentTask == null) {
            return null;
        }
        TodoTask updatedTask =
                new TodoTask(
                        id,
                        task.title(),
                        task.isCompleted(),
                        currentTask.createdAt(),
                        System.currentTimeMillis());
        todoCommands.set(id.toString(), updatedTask);
        return updatedTask;
    }

    @Override
    public boolean delete(Integer id) {
        int del = keyCommands.del(id.toString());
        return del == 1;
    }
}
