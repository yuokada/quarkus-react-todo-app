package io.github.yuokada.practice;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Deprecated
@ApplicationScoped
public class UsersResource {

    private final ReactiveKeyCommands<String> keyCommands;
    private final ValueCommands<String, Long> countCommands;

    @Inject
    public UsersResource(RedisDataSource ds, ReactiveRedisDataSource reactive) {
        countCommands = ds.value(Long.class);
        keyCommands = reactive.key();
    }

    public void incrementUserCount() {
        countCommands.set("user:count", 0L);
        // countCommands.increment("user:count");
    }

    public List<Users> getUsers() {
        try {
            // "user:*"にマッチするすべてのキーを取得する
            List<String> keys = keyCommands.keys("user:*")
                .subscribeAsCompletionStage()
                .toCompletableFuture()
                .get();  // 非同期処理が完了するまで待機

            // 結果を処理してUsersオブジェクトのリストを作成する
            List<Users> users = new ArrayList<>();
            for (String key : keys) {
                Long value = countCommands.get(key);
                Users user = new Users(key, value);
                users.add(user);
            }
            return users;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
