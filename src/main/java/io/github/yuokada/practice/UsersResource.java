package io.github.yuokada.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

@Deprecated
@ApplicationScoped
public class UsersResource {

    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, Long> countCommands;

    @Inject
    public UsersResource(RedisDataSource ds) {
        countCommands = ds.value(Long.class);
        keyCommands = ds.key();
    }

    public void incrementUserCount() {
        countCommands.set("user:count", 0L);
        // countCommands.increment("user:count");
    }

    public List<Users> getUsers() {
        try {
            List<String> keys = keyCommands.keys("user:*");
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
