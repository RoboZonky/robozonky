package net.petrovicky.zonkybot.api.remote;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MarketplaceTest {

    private static ResteasyClient CLIENT;
    private static Marketplace MARKETPLACE;

    @Test
    public void testBasicParsing() {
        Ratings ratings = Ratings.of(Rating.D);
        MARKETPLACE.getLoans(ratings, 0);
    }

    @BeforeClass
    public static void startUp() {
        CLIENT = new ResteasyClientBuilder().build();
        MARKETPLACE = CLIENT.target("https://api.zonky.cz").proxy(Marketplace.class);
    }

    @AfterClass
    public static void cleanUp() {
        CLIENT.close();
        CLIENT = null;
        MARKETPLACE = null;
    }

}
