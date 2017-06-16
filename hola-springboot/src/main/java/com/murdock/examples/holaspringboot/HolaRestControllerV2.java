package com.murdock.examples.holaspringboot;

import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConfigurationProperties(prefix = "helloapp")
public class HolaRestControllerV2 {

    private String saying;

    @RequestMapping(method = RequestMethod.GET, value = "/holaV2", produces = "text/plain")
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return saying + " @ " + hostname;
    }

    public String getSaying() {
        return saying;
    }

    public void setSaying(String saying) {
        this.saying = saying;
    }
}
