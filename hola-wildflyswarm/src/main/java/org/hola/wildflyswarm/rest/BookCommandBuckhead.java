package org.hola.wildflyswarm.rest;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

import java.util.Date;
import java.util.Random;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * @author weipeng2k 2017年09月24日 下午17:38:54
 */
public class BookCommandBuckhead extends HystrixCommand<Book> {

    private final String host;
    private final int port;
    private final Long bookId;

    public BookCommandBuckhead(String host, int port, Long bookId) {
        super(Setter.withGroupKey(
                HystrixCommandGroupKey.Factory
                        .asKey("wildflyswarm.backend.buckhead"))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withMaxQueueSize(-1)
                        .withCoreSize(5)
                )
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
        Random random = new Random();
        int i = random.nextInt(1000) + 1;
        Thread.sleep(i);

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
