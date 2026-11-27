package io.github.yuokada.practice.presentation.rest;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.common.annotation.Blocking;

import io.github.yuokada.practice.application.agent.TodoAgent;

@Path("/api/agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TodoAgentResource {

    private final TodoAgent agent;

    public TodoAgentResource(TodoAgent agent) {
        this.agent = agent;
    }

    @POST
    @Blocking
    public AgentChatResponse chat(@Valid AgentChatRequest request) {
        return new AgentChatResponse(agent.chat(request.sessionId(), request.message()));
    }
}
