package com.github.robozonky.app.daemon;

import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvestingOperationDescriptionTest extends AbstractZonkyLeveragingTest {

    @Test
    void getters() {
        final InvestingOperationDescriptor d = new InvestingOperationDescriptor();
        final Loan l = Loan.custom().build();
        final LoanDescriptor ld = new LoanDescriptor(l);
        assertThat(d.identify(ld)).isEqualTo(l.getId());
    }

    @Test
    void eliminatesUselessLoans() {
        final Loan alreadyInvested = Loan.custom()
                .setRating(Rating.B)
                .setNonReservedRemainingInvestment(1)
                .setMyInvestment(mock(MyInvestment.class))
                .build();
        final Loan normal = Loan.custom()
                .setRating(Rating.A)
                .setNonReservedRemainingInvestment(1)
                .build();
        final InvestingOperationDescriptor d = new InvestingOperationDescriptor();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getAvailableLoans(any())).thenReturn(Stream.of(alreadyInvested, normal));
        final Tenant tenant = mockTenant(zonky);
        final Stream<LoanDescriptor> ld = d.readMarketplace(tenant);
        assertThat(ld).hasSize(1)
                .element(0)
                .extracting(LoanDescriptor::item)
                .isSameAs(normal);
    }

}
