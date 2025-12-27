package io.github.yuokada.practice;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URISyntaxException;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

@Produces(MediaType.APPLICATION_JSON)
public interface TodoResource {

    @GET
    @Path("/")
    Response keys();

    @GET
    @Path("/{id}")
    Response detail(@PathParam("id") Integer id);

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    Response post(@RequestBody TodoTask task) throws URISyntaxException;

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response put(@PathParam("id") Integer id, @RequestBody TodoTask task);

    @DELETE
    @Path("/{id}")
    Response delete(@PathParam("id") Integer id);
}
