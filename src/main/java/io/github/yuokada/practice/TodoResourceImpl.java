package io.github.yuokada.practice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@ApplicationScoped
@Path("/api/todos")
@Produces(MediaType.APPLICATION_JSON)
public class TodoResourceImpl implements TodoResource {

    private final TodoService service;

    @Inject
    public TodoResourceImpl(TodoService service) {
        this.service = service;
    }

    @Override
    public Response keys() {
        var tasks = service.tasks();
        return Response.ok(tasks).build();
    }

    @Override
    public Response detail(Integer id) {
        return Response.ok(service.task(id)).build();
    }

    @Override
    public Response post(@RequestBody TodoTask task) throws URISyntaxException {
        TodoTask todoTask = service.create(task);
        var uri = new URI("/api/todos/" + todoTask.id());
        return Response.created(uri).entity(todoTask).build();
    }

    @Override
    public Response put(Integer id, @RequestBody TodoTask task) {
        return Response.ok(service.update(id, task)).build();
    }

    @Override
    public Response delete(Integer id) {
        boolean result = service.delete(id);
        if (result) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
