package org.hola.wildflyswarm.rest;

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author weipeng2k
 */
@Path("/api")
public class BookResource3 {

    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_HOST",
            defaultValue = "localhost")
    private String backendServiceHost;
    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_PORT",
            defaultValue = "8080")
    private int backendServicePort;

    @Path("/books3/{bookId}")
    @GET
    public String greeting(@PathParam("bookId") Long bookId) {
        return new BookCommandBuckhead(backendServiceHost, backendServicePort, bookId).execute().toString();
    }
}
