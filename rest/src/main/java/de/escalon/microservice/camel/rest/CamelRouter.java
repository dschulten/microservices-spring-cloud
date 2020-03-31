package de.escalon.microservice.camel.rest;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class CamelRouter extends RouteBuilder {

  @Override
  public void configure() throws Exception {
    // from("timer:hello?period={{timer.period}}")
    //    .routeId("hello")
    //    .routeGroup("hello-group")
    //    .transform()
    //    .method("myBean", "saySomething")
    //    .filter(simple("${body} contains 'foo'"))
    //    .to("log:foo")
    //    .end()
    //    .to("stream:out");

    // configure we want to use spark-rest on port 8080 as the component for the rest DSL
    // and for the swagger api-doc as well
    restConfiguration()
        // .contextPath("/rest")
        .contextPath("/rest")
        .component("servlet")
        // .component("undertow")
        .apiContextPath("/api-doc")
        .port(8080)
        // and we enable json binding mode
        .bindingMode(RestBindingMode.json)
        // and output using pretty print
        .dataFormatProperty("prettyPrint", "true");

    // this user REST service is json only
    rest("/users")
        .consumes("application/json")
        .produces("application/json")
        .get("/{id}")
        .outType(User.class)
        .to("bean:userService?method=getUser(${header.id})")
        .get()
        .outType(User[].class)
        .route()
        .log("Get users")
        .to("bean:userService?method=listUsers")
        .endRest()
        .put("/update")
        .type(User.class)
        .outType(User.class)
        .to("bean:userService?method=updateUser");
  }
}
