package io.github.yuokada.practice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.yuokada.practice.application.agent.TodoAgentTools;
import io.github.yuokada.practice.application.service.TodoService;
import io.github.yuokada.practice.domain.model.TodoTask;
import io.github.yuokada.practice.domain.repository.TodoRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodoAgentToolsTest {

    private TodoAgentTools tools;

    @BeforeEach
    void setUp() {
        tools = new TodoAgentTools(new TodoService(new InMemoryTodoRepository()));
    }

    @Test
    void managesTodosThroughApplicationService() {
        TodoTask created = tools.createTodo("AIから追加", false);

        assertEquals("AIから追加", tools.getTodo(created.id()).title());
        assertEquals(1, tools.listTodos().size());

        TodoTask updated = tools.updateTodo(created.id(), "AIから更新", true);
        assertEquals("AIから更新", updated.title());
        assertTrue(updated.completed());

        assertTrue(tools.deleteTodo(created.id()));
        assertFalse(tools.deleteTodo(created.id()));
        assertTrue(tools.listTodos().isEmpty());
    }

    private static final class InMemoryTodoRepository implements TodoRepository {

        private final Map<Integer, TodoTask> tasks = new LinkedHashMap<>();
        private int nextId = 1;

        @Override
        public List<TodoTask> findAll() {
            return List.copyOf(tasks.values());
        }

        @Override
        public TodoTask findById(Integer id) {
            return tasks.get(id);
        }

        @Override
        public TodoTask create(TodoTask task) {
            long now = System.currentTimeMillis();
            TodoTask created = new TodoTask(nextId++, task.title(), task.completed(), now, now);
            tasks.put(created.id(), created);
            return created;
        }

        @Override
        public TodoTask update(Integer id, TodoTask task) {
            TodoTask current = tasks.get(id);
            if (current == null) {
                return null;
            }
            TodoTask updated =
                    new TodoTask(id, task.title(), task.completed(), current.createdAt(), current.updatedAt() + 1);
            tasks.put(id, updated);
            return updated;
        }

        @Override
        public boolean delete(Integer id) {
            return tasks.remove(id) != null;
        }
    }
}
