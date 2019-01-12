package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchasingOperationDescriptionTest extends AbstractZonkyLeveragingTest {

    @Test
    void getters() {
        final PurchasingOperationDescriptor d = new PurchasingOperationDescriptor();
        final Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(1l);
        final Loan l = Loan.custom().build();
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(d.getRefreshInterval()).isEqualTo(Duration.ZERO);
            softly.assertThat(d.identify(pd)).isEqualTo(1);
        });
    }

    @Test
    void readsMarketplace() {
        final Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(1l);
        final PurchasingOperationDescriptor d = new PurchasingOperationDescriptor();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getAvailableParticipations(any())).thenReturn(Stream.of(p));
        final Tenant tenant = mockTenant(zonky);
        final Stream<ParticipationDescriptor> ld = d.readMarketplace(tenant);
        assertThat(ld).hasSize(1)
                .element(0)
                .extracting(ParticipationDescriptor::item)
                .isSameAs(p);
    }

}
