package io.github.yuokada.practice.infrastructure.redis;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;

import io.github.yuokada.practice.domain.repository.IncrementRepository;

@Typed(RedisIncrementRepository.class)
@ApplicationScoped
public class RedisIncrementRepository implements IncrementRepository {

    private final ReactiveKeyCommands<String> keyCommands;
    private final ValueCommands<String, Long> countCommands;

    @Inject
    public RedisIncrementRepository(RedisDataSource ds, ReactiveRedisDataSource reactive) {
        countCommands = ds.value(Long.class);
        keyCommands = reactive.key();
    }

    @Override
    public long get(String key) {
        Long value = countCommands.get(key);
        if (value == null) {
            return 0L;
        }
        return value;
    }

    @Override
    public void set(String key, long value) {
        countCommands.set(key, value);
    }

    @Override
    public void increment(String key, long incrementBy) {
        countCommands.incrby(key, incrementBy);
    }

    @Override
    public Uni<Void> delete(String key) {
        return keyCommands.del(key).replaceWithVoid();
    }

    @Override
    public Uni<List<String>> keys() {
        return keyCommands.keys("*");
    }
}
