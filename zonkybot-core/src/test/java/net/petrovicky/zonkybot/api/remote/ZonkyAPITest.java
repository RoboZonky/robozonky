package net.petrovicky.zonkybot.api.remote;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ZonkyAPITest {

    private static ResteasyClient CLIENT;
    private static ZonkyAPI API;

    @Test
    public void testBasicParsing() {
        Ratings ratings = Ratings.of(Rating.D);
        API.getLoans(ratings, 0);
    }

    @BeforeClass
    public static void startUp() {
        CLIENT = new ResteasyClientBuilder().build();
        API = CLIENT.target("https://api.zonky.cz").proxy(ZonkyAPI.class);
    }

    @AfterClass
    public static void cleanUp() {
        CLIENT.close();
        CLIENT = null;
        API = null;
    }

}
