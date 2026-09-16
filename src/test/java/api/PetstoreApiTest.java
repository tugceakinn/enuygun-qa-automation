package api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PetstoreApiTest {

    int petId = 778899;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://petstore.swagger.io/v2";
    }

    @Test(priority = 1)
    public void createPetTest() {
        String requestBody = "{\n" +
                "  \"id\": " + petId + ",\n" +
                "  \"category\": {\n" +
                "    \"id\": 1,\n" +
                "    \"name\": \"Kopek\"\n" +
                "  },\n" +
                "  \"name\": \"Karabas\",\n" +
                "  \"photoUrls\": [\n" +
                "    \"string\"\n" +
                "  ],\n" +
                "  \"tags\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"name\": \"Evcil\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"status\": \"available\"\n" +
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/pet")
                .then()
                .statusCode(200)
                .body("id", equalTo(petId))
                .body("name", equalTo("Karabas"));
    }

    @Test(priority = 2)
    public void getPetTest() {
        given()
                .pathParam("id", petId)
                .when()
                .get("/pet/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(petId))
                .body("name", equalTo("Karabas"));
    }

    @Test(priority = 3)
    public void updatePetTest() {
        String updatedBody = "{\n" +
                "  \"id\": " + petId + ",\n" +
                "  \"category\": {\n" +
                "    \"id\": 1,\n" +
                "    \"name\": \"Kopek\"\n" +
                "  },\n" +
                "  \"name\": \"Kangal\",\n" +
                "  \"photoUrls\": [\"string\"],\n" +
                "  \"tags\": [{\"id\": 1, \"name\": \"Evcil\"}],\n" +
                "  \"status\": \"sold\"\n" +
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(updatedBody)
                .when()
                .put("/pet")
                .then()
                .statusCode(200)
                .body("name", equalTo("Kangal"))
                .body("status", equalTo("sold"));
    }

    @Test(priority = 4)
    public void deletePetTest() {
        given()
                .pathParam("id", petId)
                .when()
                .delete("/pet/{id}")
                .then()
                .statusCode(200)
                .body("message", equalTo(String.valueOf(petId)));
    }

    @Test(priority = 5)
    public void negativeGetDeletedPetTest() {
        given()
                .pathParam("id", petId)
                .when()
                .get("/pet/{id}")
                .then()
                .statusCode(404)
                .body("message", equalTo("Pet not found"));
    }
}