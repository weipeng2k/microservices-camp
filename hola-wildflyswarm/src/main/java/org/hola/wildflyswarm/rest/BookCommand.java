package org.hola.wildflyswarm.rest;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * @author weipeng2k 2017年09月24日 下午17:38:54
 */
public class BookCommand extends HystrixCommand<Book> {

    private final String host;
    private final int port;
    private final Long bookId;

    public BookCommand(String host, int port, Long bookId) {
        super(Setter.withGroupKey(
                HystrixCommandGroupKey.Factory
                        .asKey("wildflyswarm.backend"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withCircuitBreakerEnabled(true)
                        .withCircuitBreakerRequestVolumeThreshold(5)
                        .withMetricsRollingStatisticalWindowInMilliseconds(5000)
                ));

        this.host = host;
        this.port = port;
        this.bookId = bookId;
    }

    @Override
    protected Book run() throws Exception {
        String backendServiceUrl = String.format("http://%s:%d",
                host, port);
        System.out.println("Sending to: " + backendServiceUrl);
        Client client = ClientBuilder.newClient();
        Book book = client.target(backendServiceUrl).path("hola-backend").path("rest").path("books").path(
                bookId.toString()).request().accept("application/json").get(Book.class);

        return book;
    }

    @Override
    protected Book getFallback() {
        Book book = new Book();
        book.setAuthorName("老中医");
        book.setId(999L);
        book.setPublishDate(new Date());
        book.setVersion(1);
        book.setName("颈椎病康复指南");

        return book;
    }
}
