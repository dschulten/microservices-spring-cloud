package de.escalon.microservice.camel.rest;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CamelRouter extends RouteBuilder {

  @Value("${server.port}")
  private int serverPort;

  @Override
  public void configure() throws Exception {
    restConfiguration()
        .contextPath("/rest")
        .component("servlet")
        //.component("undertow")
        .port(serverPort)
        .bindingMode(RestBindingMode.json)
        .dataFormatProperty("prettyPrint", "true")
        .apiContextPath("/api-doc")
        .apiProperty("api.title", "User API")
        .apiProperty("api.version", "1.0.0");

    rest("/users")
        .consumes("application/json")
        .produces("application/json")
        .get("/{id}")
        .outType(User.class)
        .to("direct:getuser")
        .get()
        .outType(User[].class).to("direct:getusers")
        .put("/update")
        .type(User.class)
        .outType(User.class)
        .to("bean:userService?method=updateUser");

    from("direct:getusers").routeId("getUsersRoute")
        .log("Get users")
        .to("bean:userService?method=listUsers");

    from("direct:getuser")
        .to("bean:userService?method=getUser(${header.id})");
  }
}
