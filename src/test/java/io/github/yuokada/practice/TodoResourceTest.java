package io.github.yuokada.practice;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TodoResourceTest {

    private static final Logger log = LoggerFactory.getLogger(TodoResourceTest.class);

    @Order(1)
    @RepeatedTest(3)
    void keys() {
        given().accept(ContentType.JSON)
            .when()
            .get("/api/todos/")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(3));
    }

    @Order(2)
    @Test
    void detail() {
        given().accept(ContentType.JSON)
            .when()
            .get("/api/todos/4")
            .then()
            .statusCode(200)
            .body("id", is(4))
            .body("completed", is(false));
    }

    @Order(3)
    @RepeatedTest(5)
    void post() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"new task from sync API\"}")
            .when()
            .post("/api/todos/")
            .then()
            .statusCode(201)
            .body("completed", is(false));
    }

    @Order(4)
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

    @Order(5)
    @RepeatedTest(2)
    void put() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"updated sync task\",\"completed\":true}")
            .when()
            .put("/api/todos/8")
            .then()
            .statusCode(200)
            .body("id", is(8))
            .body("completed", is(true));
    }

    @Order(6)
    @Test
    @DisplayName("(Flaky test) Delete existing ID should return 204 No Content")
    void delete() {
        List<TodoTask> response = given().when()
            .get("/api/todos/").as(new io.restassured.common.mapper.TypeRef<>() {
            });
        response.forEach(task -> {
            log.debug(task.toString());
        });
        TodoTask firstTask = response.stream().findFirst().orElseThrow(() ->
            new IllegalStateException("No TodoTask available to delete.")
        );
        given()
            .when()
            .delete("/api/todos/%d".formatted(firstTask.id()))
            .then()
            .statusCode(204);

    }

    @Order(7)
    @Test
    void deleteNonExistId() {
        int nonExistId = 99999;
        given()
            .when()
            .delete("/api/todos/%d".formatted(nonExistId))
            .then()
            .statusCode(404);
    }
}
