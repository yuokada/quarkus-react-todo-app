package io.github.yuokada.practice.application.service;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoRepository;

@ApplicationScoped
public class TodoService {

    private final TodoRepository repository;

    @Inject
    public TodoService(TodoRepository repository) {
        this.repository = repository;
    }

    public List<TodoTask> tasks() {
        return repository.findAll();
    }

    public TodoTask task(Integer id) {
        return repository.findById(id);
    }

    public TodoTask create(TodoTask task) {
        return repository.create(task);
    }

    public TodoTask update(Integer id, TodoTask task) {
        return repository.update(id, task);
    }

    public boolean delete(Integer id) {
        return repository.delete(id);
    }
}
