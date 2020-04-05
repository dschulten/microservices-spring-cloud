package de.escalon.microservice.camel.rest;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.engine.DefaultFluentProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.apache.camel.builder.Builder.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.cloud.consul.enabled=false")
public class CamelRouteTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private CamelContext camelContext;

  @EndpointInject("mock:bean:userService")
  private MockEndpoint mockUserService;

  private User user;

  @BeforeEach
  public void setUp() throws Exception {
    AdviceWithRouteBuilder.adviceWith(camelContext, "getUsersRoute", a -> {
      a.mockEndpointsAndSkip("bean:userService*");
    });

    user = new User();
    user.setId(1);
    user.setName("Jane");

    mockUserService.returnReplyBody(constant(new User[] {user}));
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
  public void camelStarts() {
    assertEquals(ServiceStatus.Started, camelContext.getStatus());
    assertThat(camelContext.getRoutes()).hasSizeGreaterThan(0);
  }
}
