package net.petrovicky.zonkybot;


import java.io.IOException;
import java.util.Base64;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import net.petrovicky.zonkybot.api.remote.Authorization;
import net.petrovicky.zonkybot.api.remote.Marketplace;
import net.petrovicky.zonkybot.api.remote.Token;
import net.petrovicky.zonkybot.strategy.Strategy;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operations {

    private static final String ZONKY_URL = "https://api.zonky.cz";

    private static class AuthorizationFilter implements ClientRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            LOGGER.debug("Will '{}' to '{}'.", clientRequestContext.getMethod(), clientRequestContext.getUri());
            String authCode = Base64.getEncoder().encodeToString("web:web".getBytes());
            clientRequestContext.getHeaders().add("Authorization", "Basic " + authCode);
        }
    }

    private static class AuthenticatedFilter implements ClientRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedFilter.class);

        private final Token authorization;

        public AuthenticatedFilter(final Token token) {
            this.authorization = token;
            LOGGER.debug("Using Zonky access token '{}'.", authorization.getAccessToken());
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            LOGGER.debug("Will '{}' to '{}'.", clientRequestContext.getMethod(), clientRequestContext.getUri());
            clientRequestContext.getHeaders().add("Authorization", "Bearer " + authorization.getAccessToken());
        }
    }

    private final String username, password;
    private final Strategy strategy;
    private Marketplace authenticatedClient;

    public Operations(String username, String password, Strategy strategy) {
        this.username = username;
        this.password = password;
        this.strategy = strategy;
    }

    public void login() {
        // register Jackson
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(instance);
        instance.registerProvider(ResteasyJackson2Provider.class);
        // authenticate
        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
        clientBuilder.providerFactory(instance);
        ResteasyClient client = clientBuilder.build();
        client.register(new AuthorizationFilter());
        Authorization auth = client.target(ZONKY_URL).proxy(Authorization.class);
        Token authorization = auth.login(username, password, "password", "SCOPE_APP_WEB");
        client.close();
        // provide authenticated clients
        authenticatedClient = clientBuilder.build().register(new AuthenticatedFilter(authorization))
                .target(ZONKY_URL).proxy(Marketplace.class);
    }

    public void logout() {
        authenticatedClient.logout();
    }

}
