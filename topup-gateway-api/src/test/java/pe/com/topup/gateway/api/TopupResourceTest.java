package pe.com.topup.gateway.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pe.com.topup.gateway.application.service.TopupService;
import pe.com.topup.gateway.domain.model.TopupRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TopupResourceTest {

    @InjectMock
    TopupService topupService;

    @Test
    public void testSuccessfulTopup() {
        Mockito.when(topupService.processTopup(Mockito.any(TopupRequest.class)))
                .thenReturn(Uni.createFrom().item(new TopupRequest()));

        given()
                .contentType(ContentType.JSON)
                .body("{\"phoneNumber\": \"987654321\", \"amount\": 10.5, \"carrier\": \"MOVISTAR\"}")
                .when()
                .post("/v1/topups")
                .then()
                .statusCode(202);
    }
}
