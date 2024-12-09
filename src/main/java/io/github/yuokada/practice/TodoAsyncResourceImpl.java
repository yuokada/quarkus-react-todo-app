package io.github.yuokada.practice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api/async/todos")
public class TodoAsyncResourceImpl implements TodoAsyncResource {

    private final TodoAsyncService service;
    private final Logger logger = Logger.getLogger(TodoAsyncResourceImpl.class);

    @Inject
    public TodoAsyncResourceImpl(TodoAsyncService service) {
        this.service = service;
    }

    @Override
    public CompletionStage<Response> keys() {
        var result = service.tasks();
        // Wrap the result with CompletionStage<Response>
        return result.onItem()
            .transform(list -> Response.ok(list).build())
            .subscribeAsCompletionStage()
            .exceptionally(ex -> {
                throw new RuntimeException(ex);
            });
    }

    @Override
    public CompletionStage<Response> detail(Integer id) {
        return service.asyncTask(id.toString())
            .onItem().transform(todoTask -> {
                if (todoTask == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                return Response.ok(todoTask).build();
            }).subscribeAsCompletionStage()
            .exceptionally(ex -> {
                throw new RuntimeException(ex);
            });
    }

    @Override
    public CompletionStage<Response> post(TodoTask task) {
        return service.create(task).thenApply(todoTask -> {
                try {
                    URI u = new URI("/api/async/todos/" + todoTask.id());
                    return Response.created(u).entity(todoTask).build();
                } catch (URISyntaxException e) {
                    return Response.status(201).entity(todoTask).build();
                }
            }
        ).exceptionally(ex -> {
            throw new RuntimeException(ex);
        });
    }

    @Override
    public CompletionStage<Response> put(Integer id, TodoTask task) {
        return service.updateAsync(id, task).onItem().transform(todoTask -> {
            if (todoTask == null) {
                logger.infof("Task ID not found: %d", id);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(todoTask).build();
        }).subscribeAsCompletionStage().exceptionally(ex -> {
            throw new RuntimeException(ex);
        });
    }

    @Override
    public CompletionStage<Response> delete(Integer id) {
        return service.delete(id)
            .thenApply(deleteResult -> deleteResult
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build())
            .exceptionally(ex -> {
                // Handle ExecutionException and InterruptedException in a fluent way
                throw new RuntimeException(ex);
            });
    }
}
