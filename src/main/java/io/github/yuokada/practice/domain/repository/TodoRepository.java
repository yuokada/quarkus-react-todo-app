package io.github.yuokada.practice.domain.repository;

import java.util.List;

import io.github.yuokada.practice.domain.model.TodoTask;

public interface TodoRepository {

    List<TodoTask> findAll();

    TodoTask findById(Integer id);

    TodoTask create(TodoTask task);

    TodoTask update(Integer id, TodoTask task);

    boolean delete(Integer id);
}
