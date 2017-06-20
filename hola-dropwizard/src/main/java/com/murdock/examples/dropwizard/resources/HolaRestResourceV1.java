package com.murdock.examples.dropwizard.resources;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author weipeng2k 2017年06月19日 下午16:16:30
 */
@Path("/api")
public class HolaRestResourceV1 {

    @Path("/holaV1")
    @GET
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "Hola Dropwizard @ " + hostname;
    }
}
