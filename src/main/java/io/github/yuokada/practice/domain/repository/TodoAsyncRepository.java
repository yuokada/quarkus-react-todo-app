package io.github.yuokada.practice.domain.repository;

import java.util.List;

import io.smallrye.mutiny.Uni;

import io.github.yuokada.practice.domain.model.TodoTask;

public interface TodoAsyncRepository {

    Uni<List<TodoTask>> findAll();

    Uni<TodoTask> findById(String id);

    Uni<TodoTask> create(TodoTask task);

    Uni<TodoTask> update(Integer id, TodoTask task);

    Uni<Boolean> delete(Integer id);
}
