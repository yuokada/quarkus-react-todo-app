package io.github.yuokada.practice;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TodoAsyncResourceTest {

    @Order(1)
    @RepeatedTest(2)
    void post() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"my new async task\"}")
            .when()
            .post("/api/async/todos")
            .then()
            .statusCode(201)
            .body("id", greaterThanOrEqualTo(4));
    }

    @Order(1)
    @Test
    void postWithInvalidBody() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"invalid\":\"my new async task\"}")
            .when()
            .post("/api/async/todos")
            .then()
            .statusCode(400);
    }

    @Order(3)
    @RepeatedTest(3)
    void put() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"my new async task updated\",\"completed\":false}")
            .when()
            .put("/api/async/todos/8")
            .then()
            .statusCode(200);
    }

    @Order(3)
    @Test
    void putToNonExistId() {
        int nonExistId = 100000;
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"title\":\"my dummy task\",\"completed\":false}")
            .when()
            .put("/api/async/todos/%d".formatted(nonExistId))
            .then()
            .statusCode(404);
    }


    @Order(10)
    @Test
    void delete() {
        given()
            .when()
            .delete("/api/async/todos/2")
            .then()
            .statusCode(204);
    }

    @Order(10)
    @Test
    void deleteNonExistId() {
        int nonExistId = 100000;
        // DELETE http://localhost:8080/api/todos/8
        given()
            .when()
            .delete("/api/async/todos/%d".formatted(nonExistId))
            .then()
            .statusCode(404);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.Random.class)
    class NestedTest {

        @RepeatedTest(5)
        @Timeout(value = 1500, unit = TimeUnit.MILLISECONDS)
        void keys() {
            given().accept(ContentType.JSON)
                .when()
                .get("/api/async/todos/")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(3));
        }

        @RepeatedTest(5)
        @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
        void detail() {
            given().accept(ContentType.JSON)
                .when()
                .get("/api/async/todos/1")
                .then()
                .statusCode(404);

            given().accept(ContentType.JSON)
                .when()
                .get("/api/async/todos/4")
                .then()
                .statusCode(200)
                .body("id", is(4))
                .body("completed", is(false));
        }
    }

    // @AfterAll
    static void cleanup() {
        // Sleep for n minutes
        int n = 0;
        try {
            Thread.sleep(60000 * n);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
