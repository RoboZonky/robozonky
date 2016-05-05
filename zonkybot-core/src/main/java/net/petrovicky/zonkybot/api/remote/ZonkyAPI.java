package net.petrovicky.zonkybot.api.remote;

import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/")
public interface ZonkyAPI {

    String MARKETPLACE = "/loans/marketplace";
    String ME = "/users/me";

    @GET
    @Path(ME + "/wallet")
    Wallet getWallet();

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths,
            @QueryParam("termInMonths__lte") int mostPossibleTermInMonths);

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths);

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment);

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings);

    @GET
    @Path(ME + "/logout")
    List<Loan> logout();
}
