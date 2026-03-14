package io.github.yuokada.practice;

import java.util.Map;

import org.junit.jupiter.api.Test;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import io.github.yuokada.practice.domain.model.TodoTask;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
class TodoAsyncResourceTest {

    private static final int NON_EXISTENT_ID = 100_000;

    @Test
    void keys() {
        TodoTask createdTask = createTodo("async keys task");

        given().accept(ContentType.JSON)
                .when()
                .get("/api/async/todos/")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("id", hasItem(createdTask.id()));
    }

    @Test
    void detail() {
        TodoTask createdTask = createTodo("async detail task");

        given().accept(ContentType.JSON)
                .when()
                .get("/api/async/todos/%d".formatted(createdTask.id()))
                .then()
                .statusCode(200)
                .body("id", is(createdTask.id()))
                .body("title", is("async detail task"))
                .body("completed", is(false));
    }

    @Test
    void detailNonExistId() {
        given().accept(ContentType.JSON)
                .when()
                .get("/api/async/todos/%d".formatted(NON_EXISTENT_ID))
                .then()
                .statusCode(404);
    }

    @Test
    void post() {
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"title\":\"my new async task\"}")
                .when()
                .post("/api/async/todos")
                .then()
                .statusCode(201)
                .body("title", is("my new async task"))
                .body("id", greaterThanOrEqualTo(1));
    }

    @Test
    void postWithInvalidBody() {
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"invalid\":\"my new async task\"}")
                .when()
                .post("/api/async/todos")
                .then()
                .statusCode(400);
    }

    @Test
    void put() {
        TodoTask createdTask = createTodo("async update task");

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"title\":\"my new async task updated\",\"completed\":false}")
                .when()
                .put("/api/async/todos/%d".formatted(createdTask.id()))
                .then()
                .statusCode(200)
                .body("id", is(createdTask.id()))
                .body("title", is("my new async task updated"))
                .body("completed", is(false));
    }

    @Test
    void putToNonExistId() {
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"title\":\"my dummy task\",\"completed\":false}")
                .when()
                .put("/api/async/todos/%d".formatted(NON_EXISTENT_ID))
                .then()
                .statusCode(404);
    }

    @Test
    void delete() {
        TodoTask createdTask = createTodo("async delete task");

        given().when()
                .delete("/api/async/todos/%d".formatted(createdTask.id()))
                .then()
                .statusCode(204);
    }

    @Test
    void deleteNonExistId() {
        given().when()
                .delete("/api/async/todos/%d".formatted(NON_EXISTENT_ID))
                .then()
                .statusCode(404);
    }

    private TodoTask createTodo(String title) {
        return given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(Map.of("title", title))
                .when()
                .post("/api/async/todos")
                .then()
                .statusCode(201)
                .extract()
                .as(TodoTask.class);
    }
}
