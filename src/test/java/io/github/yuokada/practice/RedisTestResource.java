package io.github.yuokada.practice;

import com.redis.testcontainers.RedisContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.utility.DockerImageName;

public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    private RedisContainer redis;

    @Override
    public Map<String, String> start() {
        redis = new RedisContainer(DockerImageName.parse("valkey/valkey:7.2.11"));
        redis.start();
        return Map.of(
            "quarkus.redis.hosts", redis.getRedisURI(),
            "quarkus.redis.load-script", "test-task.redis",
            "quarkus.redis.flush-before-load", "true",
            "quarkus.redis.devservices.enabled", "false"
        );
    }

    @Override
    public void stop() {
        if (redis != null) {
            redis.stop();
        }
    }
}
