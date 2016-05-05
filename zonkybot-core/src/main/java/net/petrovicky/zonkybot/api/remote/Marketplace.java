package net.petrovicky.zonkybot.api.remote;

import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/loans/marketplace")
@Produces("application/json")
public interface Marketplace {

    @GET
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths,
            @QueryParam("termInMonths__lte") int mostPossibleTermInMonths);

    @GET
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths);

    @GET
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment);

    @GET
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings);
}
