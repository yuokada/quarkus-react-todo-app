package io.github.yuokada.practice;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TodoService {

    private static final String ID_KEY = TodoServiceConstants.ID_KEY;

    private final ValueCommands<String, TodoTask> todoCommands;
    private final KeyCommands<String> keyCommands;

    @Inject
    public TodoService(RedisDataSource ds) {
        this.todoCommands = ds.value(TodoTask.class);
        this.keyCommands = ds.key();
    }

    public List<TodoTask> tasks() {
        List<String> keys = keyCommands.keys("*");
        return keys.stream()
            .filter(k -> k.matches("^\\d+$"))
            .map(this::task)
            .collect(Collectors.toList());
    }

    private TodoTask task(String id) {
        return todoCommands.get(id);
    }

    public TodoTask task(Integer id) {
        return todoCommands.get(id.toString());
    }

    public TodoTask create(TodoTask task) {
        try {
            Integer nextId = nextId();
            TodoTask newTask = new TodoTask(nextId, task.title(), task.isCompleted());
            todoCommands.set(nextId.toString(), newTask);
            return newTask;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer nextId() {
        // NOTE: This is not a safe way to generate unique IDs
        return (int) todoCommands.incrby(ID_KEY, 2);
    }

    public TodoTask update(Integer id, TodoTask task) {
        TodoTask currentTask = todoCommands.get(id.toString());
        if (currentTask == null) {
            return null;
        }
        TodoTask updatedTask = new TodoTask(id, task.title(), task.isCompleted());
        todoCommands.set(id.toString(), updatedTask);
        return updatedTask;
    }

    public boolean delete(Integer id) {
        int del = keyCommands.del(id.toString());
        return del == 1;
    }
}
