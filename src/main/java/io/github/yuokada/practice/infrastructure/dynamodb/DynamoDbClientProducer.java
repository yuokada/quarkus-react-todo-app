package io.github.yuokada.practice.infrastructure.dynamodb;

import java.net.URI;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.lookup.LookupIfProperty;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")
@ApplicationScoped
public class DynamoDbClientProducer {

    private static final String DUMMY_CREDENTIAL = "dummy";

    @ConfigProperty(name = "app.dynamodb.region", defaultValue = "ap-northeast-1")
    String region;

    @ConfigProperty(name = "app.dynamodb.endpoint-override")
    Optional<String> endpointOverride;

    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        var builder =
                DynamoDbClient.builder()
                        .region(Region.of(region))
                        .httpClientBuilder(UrlConnectionHttpClient.builder());
        applyLocalEndpoint(builder);
        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        var builder =
                DynamoDbAsyncClient.builder()
                        .region(Region.of(region))
                        .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        applyLocalEndpoint(builder);
        return builder.build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(
            DynamoDbAsyncClient asyncClient) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(asyncClient).build();
    }

    private void applyLocalEndpoint(AwsClientBuilder<?, ?> builder) {
        endpointOverride.ifPresent(
                ep ->
                        builder.endpointOverride(URI.create(ep))
                                .credentialsProvider(
                                        StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(
                                                        DUMMY_CREDENTIAL, DUMMY_CREDENTIAL))));
    }
}
