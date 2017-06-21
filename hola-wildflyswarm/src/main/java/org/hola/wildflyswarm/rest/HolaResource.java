package org.hola.wildflyswarm.rest;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

@Path("/api/holaV1")
public class HolaResource {

	@GET
	@Produces("text/plain")
	public Response doGet() {
		return Response.ok("method doGet invoked").build();
	}
}