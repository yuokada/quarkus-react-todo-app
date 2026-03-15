package io.github.yuokada.practice.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.yuokada.practice.domain.repository.IncrementRepository;
import io.github.yuokada.practice.domain.repository.TodoAsyncRepository;
import io.github.yuokada.practice.domain.repository.TodoRepository;
import io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbIncrementRepository;
import io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbTodoAsyncRepository;
import io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbTodoRepository;
import io.github.yuokada.practice.infrastructure.redis.RedisIncrementRepository;
import io.github.yuokada.practice.infrastructure.redis.RedisTodoAsyncRepository;
import io.github.yuokada.practice.infrastructure.redis.RedisTodoRepository;

/**
 * Produces repository beans based on the configured backend (redis or dynamodb). Each
 * implementation is annotated with {@code @Typed} so it is not registered as the interface type,
 * preventing CDI ambiguity. This producer selects the active implementation at runtime.
 */
@ApplicationScoped
public class RepositoryProducer {

    @ConfigProperty(name = "app.repository.type", defaultValue = "redis")
    String type;

    @Inject Instance<RedisTodoRepository> redisTodo;

    @Inject Instance<DynamoDbTodoRepository> dynamoDbTodo;

    @Inject Instance<RedisTodoAsyncRepository> redisTodoAsync;

    @Inject Instance<DynamoDbTodoAsyncRepository> dynamoDbTodoAsync;

    @Inject Instance<RedisIncrementRepository> redisIncrement;

    @Inject Instance<DynamoDbIncrementRepository> dynamoDbIncrement;

    @Produces
    @ApplicationScoped
    public TodoRepository todoRepository() {
        return isDynamoDb() ? dynamoDbTodo.get() : redisTodo.get();
    }

    @Produces
    @ApplicationScoped
    public TodoAsyncRepository todoAsyncRepository() {
        return isDynamoDb() ? dynamoDbTodoAsync.get() : redisTodoAsync.get();
    }

    @Produces
    @ApplicationScoped
    public IncrementRepository incrementRepository() {
        return isDynamoDb() ? dynamoDbIncrement.get() : redisIncrement.get();
    }

    private boolean isDynamoDb() {
        return "dynamodb".equals(type);
    }
}
