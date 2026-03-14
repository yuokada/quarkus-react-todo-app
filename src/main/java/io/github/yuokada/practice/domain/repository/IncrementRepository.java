package io.github.yuokada.practice.domain.repository;

import java.util.List;

import io.smallrye.mutiny.Uni;

public interface IncrementRepository {

    long get(String key);

    void set(String key, long value);

    void increment(String key, long incrementBy);

    Uni<Void> delete(String key);

    Uni<List<String>> keys();
}
