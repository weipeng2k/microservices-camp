package com.murdock.examples.holaspringboot;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author weipeng2k 2017年06月15日 下午15:19:06
 */
@RestController
@RequestMapping("/api")
public class HolaRestControllerV1 {

    @RequestMapping(method = RequestMethod.GET, value = "/holaV1", produces = "text/plain")
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "Hola Spring Boot @ " + hostname;
    }
}
