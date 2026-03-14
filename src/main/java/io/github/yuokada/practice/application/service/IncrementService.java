package io.github.yuokada.practice.application.service;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import io.github.yuokada.practice.domain.repository.IncrementRepository;

@ApplicationScoped
public class IncrementService {

    private final IncrementRepository repository;

    @Inject
    public IncrementService(IncrementRepository repository) {
        this.repository = repository;
    }

    public long get(String key) {
        return repository.get(key);
    }

    public void set(String key, long value) {
        repository.set(key, value);
    }

    public void increment(String key, long incrementBy) {
        repository.increment(key, incrementBy);
    }

    public Uni<Void> del(String key) {
        return repository.delete(key);
    }

    public Uni<List<String>> keys() {
        return repository.keys();
    }
}
