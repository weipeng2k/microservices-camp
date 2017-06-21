package org.hola.wildflyswarm.rest;

import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/api/holaV2")
public class HolaResource2 {

    @Inject
    @ConfigProperty(name = "WF_SWARM_SAYING", defaultValue = "Hola")
    private String saying;

    @GET
    @Produces("text/plain")
    public Response doGet() {
        return Response.ok(saying + " from WF Swarm").build();
    }
}