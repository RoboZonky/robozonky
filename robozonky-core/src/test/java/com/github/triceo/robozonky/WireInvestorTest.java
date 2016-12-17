package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.util.Optional;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.events.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.events.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.events.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.events.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * This test mocks all the possible outcomes of a single investment, so that the higher-level investing testing can
 * assume no problems.
 */
public class WireInvestorTest extends AbstractInvestingTest {

    @Test
    public void underBalance() {
        final BigDecimal currentBalance = BigDecimal.valueOf(0);
        final Optional<Recommendation> recommendation =
                AbstractInvestingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        final Optional<Investment> result = Investor.actuallyInvest(recommendation.get(), null, currentBalance);
        // verify result
        Assertions.assertThat(result).isEmpty();
        // verify events
        Mockito.verify(this.getListener(), Mockito.never()).handle(ArgumentMatchers.any());
    }

    @Test(expected = IllegalStateException.class)
    public void investmentFailed() {
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r))).thenReturn(new ZonkyResponse(ZonkyResponseType.FAILED));
        Investor.actuallyInvest(r, api, BigDecimal.valueOf(10000));
    }

    @Test
    public void investmentRejected() {
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r))).thenReturn(new ZonkyResponse(ZonkyResponseType.REJECTED));
        final Optional<Investment> result = Investor.actuallyInvest(r, api, BigDecimal.valueOf(10000));
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentRequestedEvent.class));
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentRejectedEvent.class));
    }

    @Test
    public void investmentDelegated() {
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r))).thenReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED));
        final Optional<Investment> result = Investor.actuallyInvest(r, api, BigDecimal.valueOf(10000));
        Assertions.assertThat(result).isEmpty();
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentRequestedEvent.class));
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentDelegatedEvent.class));
    }

    @Test
    public void investmentSuccessful() {
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r))).thenReturn(new ZonkyResponse(200));
        final Optional<Investment> result = Investor.actuallyInvest(r, api, BigDecimal.valueOf(10000));
        Assertions.assertThat(result).isPresent();
        final Investment investment = result.get();
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(investment.getAmount()).isEqualTo(r.getRecommendedInvestmentAmount());
        softly.assertThat(investment.getLoanId()).isEqualTo(r.getLoanDescriptor().getLoan().getId());
        softly.assertAll();
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentRequestedEvent.class));
        Mockito.verify(this.getListener(), Mockito.times(1))
                .handle(ArgumentMatchers.any(InvestmentMadeEvent.class));

    }

}
