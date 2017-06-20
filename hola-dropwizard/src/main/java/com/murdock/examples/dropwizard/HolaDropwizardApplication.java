package com.murdock.examples.dropwizard;

import com.murdock.examples.dropwizard.resources.GreeterRestResource;
import com.murdock.examples.dropwizard.resources.GreeterSayingFactory;
import com.murdock.examples.dropwizard.resources.HolaRestResourceV1;
import com.murdock.examples.dropwizard.resources.HolaRestResourceV2;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.client.Client;

public class HolaDropwizardApplication extends Application<HolaDropwizardConfiguration> {

    public static void main(final String[] args) throws Exception {
        new HolaDropwizardApplication().run(args);
    }

    @Override
    public String getName() {
        return "HolaDropwizard";
    }

    @Override
    public void initialize(final Bootstrap<HolaDropwizardConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final HolaDropwizardConfiguration configuration,
                    final Environment environment) {
        environment.jersey().register(new HolaRestResourceV1());
        environment.jersey().register(new HolaRestResourceV2(configuration.getSayingFactory().getSaying()));

        // greeter service
        GreeterSayingFactory greeterSayingFactory = configuration.getGreeterSayingFactory();
        Client greeterClient =
                new JerseyClientBuilder(environment)
                        .using(greeterSayingFactory.getJerseyClientConfig()).build("greeterClient");
        environment.jersey().register(new GreeterRestResource(
                greeterSayingFactory.getSaying(),
                greeterSayingFactory.getHost(),
                greeterSayingFactory.getPort(), greeterClient));

    }

}
