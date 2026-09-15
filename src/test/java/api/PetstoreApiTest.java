package api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PetstoreApiTest {

    // Testler boyunca aynı ID'yi kullanmak için global bir değişken tanımlıyoruz
    int petId = 778899;

    @BeforeClass
    public void setup() {
        // Tüm testler için temel (Base) URL'i belirliyoruz
        RestAssured.baseURI = "https://petstore.swagger.io/v2";
    }

    @Test(priority = 1)
    public void createPetTest() {
        // API'ye göndereceğimiz JSON gövdesi (Payload)
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
                .post("/pet") // Endpoint'e POST isteği atıyoruz
                .then()
                .statusCode(200) // Başarılı olup olmadığını kontrol ediyoruz
                .body("id", equalTo(petId)) // Dönen yanıttaki ID'nin bizimkiyle eşleştiğini doğruluyoruz
                .body("name", equalTo("Karabas")); // İsmin doğru kaydedildiğini doğruluyoruz

        System.out.println("Yeni Pet başarıyla oluşturuldu!");
    }

    @Test(priority = 2)
    public void getPetTest() {
        // READ: Oluşturduğumuz Pet'i ID'si ile çağırıp doğruluyoruz
        given()
                .pathParam("id", petId) // Endpoint'teki {id} değişkenine kendi petId'mizi atıyoruz
                .when()
                .get("/pet/{id}") // GET isteği atıyoruz
                .then()
                .statusCode(200) // 200 OK bekliyoruz
                .body("id", equalTo(petId))
                .body("name", equalTo("Karabas")); // İsmin hala Karabas olduğunu doğruluyoruz

        System.out.println("Pet başarıyla getirildi (GET) ve veriler doğrulandı!");
    }

    @Test(priority = 3)
    public void updatePetTest() {
        // UPDATE: İsmi 'Karabas' olan köpeğimizi 'Kangal' olarak güncelliyoruz
        String updatedBody = "{\n" +
                "  \"id\": " + petId + ",\n" +
                "  \"category\": {\n" +
                "    \"id\": 1,\n" +
                "    \"name\": \"Kopek\"\n" +
                "  },\n" +
                "  \"name\": \"Kangal\",\n" + // İsim güncellendi
                "  \"photoUrls\": [\"string\"],\n" +
                "  \"tags\": [{\"id\": 1, \"name\": \"Evcil\"}],\n" +
                "  \"status\": \"sold\"\n" + // Durumu satıldı olarak değiştirildi
                "}";

        given()
                .contentType(ContentType.JSON)
                .body(updatedBody)
                .when()
                .put("/pet") // Güncelleme için PUT isteği atıyoruz
                .then()
                .statusCode(200)
                .body("name", equalTo("Kangal")) // Yeni ismin Kangal olarak döndüğünü doğruluyoruz
                .body("status", equalTo("sold"));

        System.out.println("Pet bilgileri başarıyla güncellendi (PUT)!");
    }

    @Test(priority = 4)
    public void deletePetTest() {
        // DELETE: İşimiz bittiğinde test datasını temizliyoruz (Silme işlemi)
        given()
                .pathParam("id", petId)
                .when()
                .delete("/pet/{id}") // DELETE isteği atıyoruz
                .then()
                .statusCode(200) // Silme başarılı onayı alıyoruz
                .body("message", equalTo(String.valueOf(petId))); // Swagger genelde silinen ID'yi mesaj olarak döner

        System.out.println("Pet başarıyla silindi (DELETE)!");
    }

    @Test(priority = 5)
    public void negativeGetDeletedPetTest() {
        // NEGATİF SENARYO: Silinmiş (artık sistemde olmayan) bir pet'i getirmeye çalışıyoruz
        given()
                .pathParam("id", petId) // Az önce sildiğimiz ID'yi tekrar soruyoruz
                .when()
                .get("/pet/{id}") // GET isteği atıyoruz
                .then()
                .statusCode(404) // Başarı (200) değil, Bulunamadı (404) hatası BEKLİYORUZ
                .body("message", equalTo("Pet not found")); // Gelen hata mesajının doğruluğunu teyit ediyoruz

        System.out.println("Negatif Test Başarılı: Silinmiş pet için API doğru şekilde 404 hatası verdi!");
    }
}