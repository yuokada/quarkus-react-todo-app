package io.github.yuokada.practice;

import org.junit.jupiter.api.Test;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
class TodoAgentResourceTest {

    @Test
    void rejectsBlankMessage() {
        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {"sessionId":"test-session","message":""}
                        """)
                .when()
                .post("/api/agent")
                .then()
                .statusCode(400);
    }
}
