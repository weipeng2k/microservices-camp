package org.hola.wildflyswarm.rest;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import io.fabric8.kubeflix.ribbon.KubernetesClientConfig;
import io.fabric8.kubeflix.ribbon.KubernetesServerList;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import rx.Observable;

import java.util.Arrays;
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
public class BookResource4 {

    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_HOST",
            defaultValue = "localhost")
    private String backendServiceHost;
    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_PORT",
            defaultValue = "8080")
    private int backendServicePort;

    private String useKubernetesDiscovery;

    private ILoadBalancer loadBalancer;
    private IClientConfig config;

    public BookResource4() {
        this.config = new KubernetesClientConfig();
        this.config.loadProperties("hola-backend");

        this.useKubernetesDiscovery = System.getenv("USE_KUBERNETES_DISCOVERY");
        System.out.println("Value of USE_KUBERNETES_DISCOVERY: " + useKubernetesDiscovery);

        if ("true".equalsIgnoreCase(useKubernetesDiscovery)) {
            System.out.println("Using Kubernetes discovery for ribbon...");
            loadBalancer = LoadBalancerBuilder.newBuilder()
                    .withDynamicServerList(new KubernetesServerList(config))
                    .buildDynamicServerListLoadBalancer();
        }
    }

    @Path("/books4/{bookId}")
    @GET
    public String greeting(@PathParam("bookId") Long bookId) {
        if (loadBalancer == null) {
            System.out.println("Using a static list for ribbon");
            Server server = new Server(backendServiceHost, backendServicePort);
            loadBalancer = LoadBalancerBuilder.newBuilder()
                    .buildFixedServerListLoadBalancer(Arrays.asList(server));
        }

        Book book = LoadBalancerCommand.<Book>builder()
                .withLoadBalancer(loadBalancer)
                .build()
                .submit(server -> {
                    String backendServiceUrl = String.format("http://%s:%d", server.getHost(),
                            server.getPort());
                    System.out.println("Sending to: " + backendServiceUrl);

                    Client client = ClientBuilder.newClient();
                    return Observable.just(client.target(backendServiceUrl)
                            .path("hola-backend")
                            .path("rest")
                            .path("books")
                            .path(bookId.toString())
                            .request()
                            .accept("application/json")
                            .get(Book.class)
                    );
                }).toBlocking().first();
        return book.toString();
    }
}
