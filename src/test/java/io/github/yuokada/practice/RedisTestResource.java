package io.github.yuokada.practice;

import com.redis.testcontainers.RedisContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.utility.DockerImageName;

public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String VALKEY_IMAGE = "valkey/valkey:7.2.12";

    private RedisContainer redis;

    @Override
    public Map<String, String> start() {
        redis = new RedisContainer(DockerImageName.parse(VALKEY_IMAGE));
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
