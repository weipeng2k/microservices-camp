package com.murdock.examples.holaspringboot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author weipeng2k 2017年06月18日 下午21:55:31
 */
@RestController
@RequestMapping("/api")
@ConfigurationProperties(prefix = "books")
public class BookRestController {

    private RestTemplate template = new RestTemplate();

    private String backendHost;

    private int backendPort;

    @RequestMapping(value = "/books/{bookId}",
            method = RequestMethod.GET, produces = "text/plain")
    public String greeting(@PathVariable("bookId") Long bookId) {
        String backendServiceUrl = String.format("http://%s:%d/hola-backend/rest/books/{bookId}", backendHost, backendPort);
        Map object = template.getForObject(backendServiceUrl, Map.class, bookId);
        return object.toString();
    }

    public String getBackendHost() {
        return backendHost;
    }

    public void setBackendHost(String backendHost) {
        this.backendHost = backendHost;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public void setBackendPort(int backendPort) {
        this.backendPort = backendPort;
    }
}
