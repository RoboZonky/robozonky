package net.petrovicky.zonkybot.remote;

import java.util.List;

import org.assertj.core.api.Assertions;
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
        final Ratings ratings = Ratings.all();
        final List<Loan> loans = API.getLoans(ratings, 200);
        Assertions.assertThat(loans).isNotEmpty();
        Assertions.assertThat(loans.get(0).getRemainingInvestment()).isGreaterThan(0);
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
