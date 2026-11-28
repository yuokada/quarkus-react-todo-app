package io.github.yuokada.practice.application.agent;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import io.github.yuokada.practice.application.service.TodoService;
import io.github.yuokada.practice.domain.model.TodoTask;

@ApplicationScoped
public class TodoAgentTools {

    private final TodoService todoService;

    public TodoAgentTools(TodoService todoService) {
        this.todoService = todoService;
    }

    @Tool("List all Todo tasks")
    public List<TodoTask> listTodos() {
        return todoService.tasks();
    }

    @Tool("Get one Todo task by ID")
    public TodoTask getTodo(@P("Todo ID") int id) {
        return todoService.task(id);
    }

    @Tool("Create a Todo task")
    public TodoTask createTodo(
            @P("Task title") String title, @P("Whether the task is already completed") boolean completed) {
        return todoService.create(new TodoTask(null, title, completed, 0, 0));
    }

    @Tool("Update the title and completion state of an existing Todo task")
    public TodoTask updateTodo(
            @P("Todo ID") int id,
            @P("New task title") String title,
            @P("New completion state") boolean completed) {
        return todoService.update(id, new TodoTask(id, title, completed, 0, 0));
    }

    @Tool("Delete a Todo task after the user has explicitly confirmed its ID")
    public boolean deleteTodo(@P("Confirmed Todo ID") int id) {
        return todoService.delete(id);
    }
}
