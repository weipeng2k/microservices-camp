package org.hola.wildflyswarm.rest;

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * @author weipeng2k
 */
@Path("/api")
public class BookResource {

    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_HOST",
            defaultValue = "localhost")
    private String backendServiceHost;
    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_PORT",
            defaultValue = "8080")
    private int backendServicePort;

    @Path("/books/{bookId}")
    @GET
    public String greeting(@PathParam("bookId") Long bookId) {
        String backendServiceUrl = String.format("http://%s:%d",
                backendServiceHost, backendServicePort);
        System.out.println("Sending to: " + backendServiceUrl);
        Client client = ClientBuilder.newClient();
        Book book = client.target(backendServiceUrl).path("hola-backend").path("rest").path("books").path(
                bookId.toString()).request().accept("application/json").get(Book.class);

        if (book != null) {
            return book.toString();
        } else {
            return "not exist!";
        }
    }
}
