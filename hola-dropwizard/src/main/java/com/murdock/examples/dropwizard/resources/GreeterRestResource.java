package com.murdock.examples.dropwizard.resources;

import com.codahale.metrics.annotation.Timed;

import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;

/**
 * @author weipeng2k 2017年06月20日 上午11:12:49
 */
@Path("/api")
public class GreeterRestResource {
    private String saying;
    private String backendServiceHost;
    private int backendServicePort;
    private Client client;

    public GreeterRestResource(final String saying, String host, int port, Client client) {
        this.saying = saying;
        this.backendServiceHost = host;
        this.backendServicePort = port;
        this.client = client;
    }

    @Path("/greeting/{bookId}")
    @GET
    @Timed
    public String greeting(@PathParam("bookId") Long bookId) {
        String backendServiceUrl =
                String.format("http://%s:%d",
                        backendServiceHost, backendServicePort);

        Map map = client.target(backendServiceUrl).path("hola-backend").path("rest").path("books").path(
                bookId.toString()).request().accept("application/json").get(Map.class);

        return map.toString();
    }
}
