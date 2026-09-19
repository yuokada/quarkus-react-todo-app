package io.github.yuokada.practice.infrastructure.dynamodb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

final class DynamoDbUtils {

    private DynamoDbUtils() {}

    /**
     * Bridges a reactive-streams {@link Publisher} (as returned by the DynamoDB Enhanced Client's
     * async scan) to a {@link CompletableFuture}. Mutiny 2.x's {@code Multi.createFrom().publisher()}
     * only accepts {@code java.util.concurrent.Flow.Publisher}, so this helper is needed to collect
     * all pages into a list before handing control back to Mutiny.
     */
    static <T> CompletableFuture<List<T>> collectToList(Publisher<T> publisher) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        publisher.subscribe(
                new Subscriber<T>() {
                    private final List<T> items = new ArrayList<>();

                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(T item) {
                        items.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        future.complete(items);
                    }
                });
        return future;
    }
}
