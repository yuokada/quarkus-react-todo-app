package io.github.yuokada.practice;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
class TodoResourceTest {

    private static final int NON_EXISTENT_ID = 99_999;

    @Test
    void keys() {
        TodoTask createdTask = createTodo("sync keys task");

        given().accept(ContentType.JSON)
            .when()
            .get("/api/todos/")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("id", hasItem(createdTask.id()));
    }

    @Test
    void detail() {
        TodoTask createdTask = createTodo("sync detail task");

        given().accept(ContentType.JSON)
            .when()
            .get("/api/todos/%d".formatted(createdTask.id()))
            .then()
            .statusCode(200)
            .body("id", is(createdTask.id()))
            .body("title", is("sync detail task"))
            .body("completed", is(false));
    }

    @Test
    void post() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"new task from sync API\"}")
            .when()
            .post("/api/todos/")
            .then()
            .statusCode(201)
            .body("title", is("new task from sync API"))
            .body("completed", is(false));
    }

    @Test
    void postWithInvalidBody() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"completed\":true}")
            .when()
            .post("/api/todos/")
            .then()
            .statusCode(400);
    }

    @Test
    void put() {
        TodoTask createdTask = createTodo("sync update task");

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"updated sync task\",\"completed\":true}")
            .when()
            .put("/api/todos/%d".formatted(createdTask.id()))
            .then()
            .statusCode(200)
            .body("id", is(createdTask.id()))
            .body("title", is("updated sync task"))
            .body("completed", is(true));
    }

    @Test
    void delete() {
        TodoTask createdTask = createTodo("sync delete task");

        given()
            .when()
            .delete("/api/todos/%d".formatted(createdTask.id()))
            .then()
            .statusCode(204);
    }

    @Test
    void deleteNonExistId() {
        given()
            .when()
            .delete("/api/todos/%d".formatted(NON_EXISTENT_ID))
            .then()
            .statusCode(404);
    }

    private TodoTask createTodo(String title) {
        return given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of("title", title))
            .when()
            .post("/api/todos/")
            .then()
            .statusCode(201)
            .extract()
            .as(TodoTask.class);
    }
}
