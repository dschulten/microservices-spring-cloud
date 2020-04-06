package de.escalon.microservice.camel.rest;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.engine.DefaultFluentProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyUris;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;
import static io.restassured.RestAssured.given;
import static org.apache.camel.builder.Builder.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.cloud.consul.enabled=false")
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
public class CamelRouteTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CamelContext camelContext;

  @EndpointInject("mock:bean:userService")
  private MockEndpoint mockUserService;

  private User user;

  private RequestSpecification documentationSpec;

  @LocalServerPort
  private int port;

  @BeforeEach
  public void setUp(RestDocumentationContextProvider restDocumentation) throws Exception {
    AdviceWithRouteBuilder.adviceWith(camelContext, "getUsersRoute", a -> {
      a.mockEndpointsAndSkip("bean:userService*");
    });

    user = new User();
    user.setId(1);
    user.setName("Jane");

    mockUserService.returnReplyBody(constant(new User[] {user}));

    this.documentationSpec = new RequestSpecBuilder()
        .addFilter(documentationConfiguration(restDocumentation)).build();
  }

  @Test
  public void callsRestWithMock() {
    ResponseEntity<User[]> response = restTemplate.getForEntity("/rest/users", User[].class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    User[] s = response.getBody();
    assertThat(s).contains(user);
  }

  @Test
  public void callsDirectRouteWithMock() throws Exception {
    User[] users = DefaultFluentProducerTemplate.on(camelContext)
        .to("direct:getusers")
        .request(User[].class);
    assertThat(users).contains(user);
  }

  @Test
  public void restAssuredGeneratesRestdocs() throws Exception {
    given(this.documentationSpec)
        .accept("text/plain")
        .filter(document("sample",
            preprocessRequest(modifyUris()
                .scheme("https")
                .host("api.example.com")
                .removePort())))
        .when()
        .port(this.port)
        .get("/rest/users")
        .then()
        .assertThat().statusCode(is(200));
  }

  @Test
  public void camelStarts() {
    assertEquals(ServiceStatus.Started, camelContext.getStatus());
    assertThat(camelContext.getRoutes()).hasSizeGreaterThan(0);
  }
}
