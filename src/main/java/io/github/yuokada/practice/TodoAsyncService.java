package io.github.yuokada.practice;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TodoAsyncService {

    private static final String ID_KEY = TodoServiceConstants.ID_KEY;

    private final ReactiveValueCommands<String, TodoTask> todoCommands;
    private final ReactiveKeyCommands<String> keyCommands;
    private final Logger logger;

    @Inject
    public TodoAsyncService(ReactiveRedisDataSource ds, Logger logger) {
        this.todoCommands = ds.value(TodoTask.class);
        this.keyCommands = ds.key();
        this.logger = logger;
    }

    public Uni<TodoTask> asyncTask(String id) {
        // Get the task asynchronously without using the task() method
        return todoCommands.get(id)
            .onFailure()
            .retry()
            .atMost(5)
            ;
    }

    public Uni<List<TodoTask>> tasks() {
        return keyCommands.keys("*")
            .onItem()
            .transform(keys -> keys
                .stream()
                .filter(k -> k.matches("^\\d+$")) // Filter keys that are integers
                .collect(Collectors.toList()))
            .onItem().invoke(keys -> logger.infof("Task IDs: %s", keys))
            .onItem()
            .transformToMulti(keys -> Multi.createFrom().items(keys.stream())) // キーをストリームに変換
            .onItem()
            .transformToUniAndMerge(todoCommands::get)             // 各キーに対して非同期取得
            .onItem().invoke(task -> logger.infof("Task detail: %s", task))
            .collect().asList();
    }

    private TodoTask syncTask(String id) {
        return todoCommands.get(id).await().indefinitely();
        // return todoCommands.get(id).subscribeAsCompletionStage().toCompletableFuture().join();
    }

    //    public CompletionStage<TodoTask> create(TodoTask task) {
//        return nextId()
//            .thenCompose(nextId -> {
//                logger.infof("Next ID: %d", nextId);
//                TodoTask newTask = new TodoTask(nextId, task.title(), task.isCompleted());
//                logger.info(newTask);
//                return todoCommands.set(nextId.toString(),
//                        newTask)
//                    .subscribeAsCompletionStage()
//                    .thenCompose((v) -> {
//                        logger.info("Task created");
//
//                        return todoCommands.get(nextId.toString())
//                            .subscribeAsCompletionStage().toCompletableFuture();
//                    });
//            });
//    }
    public CompletionStage<TodoTask> create(TodoTask task) {
        return nextId()
            .thenApply(nextId -> {
                logger.infof("Next ID: %d", nextId);
                return new TodoTask(nextId, task.title(), task.isCompleted());
            })
            .thenCompose(newTask ->
                todoCommands.set(newTask.id().toString(), newTask)
                    .subscribeAsCompletionStage()
                    .thenApply(v -> {
                        logger.info("Task created");
                        return newTask;
                    })
            )
            .thenCompose(newTask ->
                todoCommands.get(newTask.id().toString())
                    .subscribeAsCompletionStage()
                    .toCompletableFuture()
            );
    }

//    private Integer nextIdOld() throws ExecutionException, InterruptedException {
//        // NOTE: This is not a safe way to generate unique IDs
//        Long result = todoCommands.incrby(ID_KEY, 2).subscribeAsCompletionStage().get();
//        return result.intValue();
//    }

    private CompletionStage<Integer> nextId() {
        return todoCommands
            .incrby(ID_KEY, 2)
            .subscribeAsCompletionStage()
            .thenApply(Long::intValue);
    }

    public TodoTask update(Integer id, TodoTask task) {
        TodoTask currentTask = syncTask(id.toString());
        if (currentTask == null) {
            return null;
        }
        TodoTask updatedTask = new TodoTask(id, task.title(), task.isCompleted());
        todoCommands.set(id.toString(), updatedTask);
        return updatedTask;
    }

    public Uni<TodoTask> updateAsync(Integer id, TodoTask task) {
        return asyncTask(id.toString())
            .onItem()
            .transformToUni(currentTask -> {
                if (currentTask == null) {
                    return Uni.createFrom().nullItem();
                }
                TodoTask updatedTask = new TodoTask(id, task.title(), task.isCompleted());
                return todoCommands.set(id.toString(), updatedTask)
                    .onItem().transform(v -> updatedTask);
            });
    }

    public CompletableFuture<Boolean> delete(Integer id) {
        Uni<Boolean> uniResult = keyCommands.del(id.toString()).map(l -> l == 1L);
        return uniResult
            .subscribeAsCompletionStage()
            .exceptionally(ex -> {
                if (ex instanceof ExecutionException || ex instanceof InterruptedException) {
                    throw new RuntimeException(ex);
                }
                throw new CompletionException(ex);
            });
    }
}
