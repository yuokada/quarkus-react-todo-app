package io.github.yuokada.practice.application.service;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoAsyncRepository;

@ApplicationScoped
public class TodoAsyncService {

    private final TodoAsyncRepository repository;

    @Inject
    public TodoAsyncService(TodoAsyncRepository repository) {
        this.repository = repository;
    }

    public Uni<TodoTask> asyncTask(String id) {
        return repository.findById(id);
    }

    public Uni<List<TodoTask>> tasks() {
        return repository.findAll();
    }

    public Uni<TodoTask> create(TodoTask task) {
        return repository.create(task);
    }

    public Uni<TodoTask> updateAsync(Integer id, TodoTask task) {
        return repository.update(id, task);
    }

    public Uni<Boolean> delete(Integer id) {
        return repository.delete(id);
    }
}
