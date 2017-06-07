/*
 * Copyright 2017 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.triceo.robozonky.app.investing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.api.confirmations.Confirmation;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.ConfirmationType;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.common.remote.AuthenticatedZonky;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * There are the following kinds of proxies:
 * <ul>
 *     <li>DRY: One in a dry run.</li>
 *     <li>SIMPLE: One that has no way of obtaining external investing input.</li>
 *     <li>CONFIRMING: One that knows to obtain external investing input.</li>
 * </ul>
 *
 * There are following kinds of investments:
 * <ul>
 *     <li>CONFIRMED: Require confirmation, and therefore requiring a CONFIRMING proxy. In case of
 *     positive confirmation, we can invest locally.</li>
 *     <li>UNCONFIRMED: Not requiring confirmation and not CAPTCHA-protected, these can be invested locally.</li>
 * </ul>
 *
 * Further, investments can be protected by CAPTCHA:
 * <ul>
 *     <li>PROTECTED: Can not invest locally, require delegation and therefore a CONFIRMING proxy.</li>
 *     <li>UNPROTECTED: Can invest locally, SIMPLE proxy will be enough provided that investment is not CONFIRMED.</li>
 * </ul>
 *
 * Based on that, these are the possible states that need to be tested:
 * <ul>
 *     <li>DRY proxy, any investment, regardless of captcha. Always accepted</li>
 *     <li>SIMPLE proxy:
 *         <ul>
 *             <li>Rejects PROTECTED investments since we cannot bypass CAPTCHA.</li>
 *             <li>Fails on CONFIRMED investments since we cannot reach confirmation.</li>
 *             <li>Fails on network issues.</li>
 *             <li>Otherwise invests locally.</li>
 *         </ul>
 *     </li>
 *     <li>CONFIRMING proxy:
 *         <ul>
 *             <li>Delegates PROTECTED investments.</li>
 *             <li>Invests UNPROTECTED CONFIRMED investments locally or rejects them.</li>
 *             <li>Invests UNPROTECTED UNCONFIRMED investments locally.</li>
 *             <li>Fails on network issues.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * This test aims to test all of these various states.
 */
@RunWith(Parameterized.class)
public class InvestorTest extends AbstractInvestingTest {

    private enum ProxyType {

        DRY, SIMPLE, CONFIRMING

    }

    private enum Captcha {

        PROTECTED, UNPROTECTED
    }

    private enum Remote {

        CONFIRMED, UNCONFIRMED
    }

    private enum RemoteResponse {

        PRESENT, ABSENT
    }

    @Parameterized.Parameters(name = "{0}+{1}+{2}({3})={4}")
    public static Collection<Object[]> generatePossibilities() {
        final Collection<Object[]> result = new ArrayList<>();
        // dry proxy
        for (final InvestorTest.Captcha c1: InvestorTest.Captcha.values()) {
            for (final InvestorTest.Remote c2: InvestorTest.Remote.values()) {
                result.add(new Object[] {InvestorTest.ProxyType.DRY, c1, c2, null, ZonkyResponseType.INVESTED});
            }
        }
        // simple proxy
        result.add(new Object[] {InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.PROTECTED,
                InvestorTest.Remote.CONFIRMED, null, null});
        result.add(new Object[] {InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.PROTECTED,
                InvestorTest.Remote.UNCONFIRMED, null, ZonkyResponseType.REJECTED});
        result.add(new Object[] {InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.UNPROTECTED,
                InvestorTest.Remote.CONFIRMED, null, null});
        result.add(new Object[] {InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.UNPROTECTED,
                InvestorTest.Remote.UNCONFIRMED, null, ZonkyResponseType.INVESTED});
        // confirming proxy, response present
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.PRESENT, ZonkyResponseType.DELEGATED});
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.PRESENT, ZonkyResponseType.DELEGATED});
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.PRESENT, ZonkyResponseType.INVESTED});
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.PRESENT, ZonkyResponseType.INVESTED});
        // confirming proxy, network failure
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.ABSENT, null});
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.ABSENT, null});
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.ABSENT, null});
        result.add(new Object[] {InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.ABSENT, ZonkyResponseType.INVESTED});
        return Collections.unmodifiableCollection(result);
    }

    private static final double LOAN_AMOUNT = 2000.0;
    private static final int CONFIRMED_AMOUNT = (int) (InvestorTest.LOAN_AMOUNT / 2);

    private static LoanDescriptor mockLoanDescriptor(final boolean protectByCaptcha) {
        final Loan loan = AbstractInvestingTest.mockLoan();
        return new LoanDescriptor(loan, protectByCaptcha ? Duration.ofHours(1) : Duration.ZERO);
    }

    @Parameterized.Parameter
    public InvestorTest.ProxyType proxyType;
    @Parameterized.Parameter(1)
    public InvestorTest.Captcha captcha;
    @Parameterized.Parameter(2)
    public InvestorTest.Remote confirmation;
    @Parameterized.Parameter(3)
    public InvestorTest.RemoteResponse confirmationResponse;
    @Parameterized.Parameter(4)
    public ZonkyResponseType responseType;

    private Recommendation getRecommendation() {
        final LoanDescriptor ld =
                InvestorTest.mockLoanDescriptor(captcha == InvestorTest.Captcha.PROTECTED);
        return ld.recommend(InvestorTest.CONFIRMED_AMOUNT,
                confirmation == InvestorTest.Remote.CONFIRMED).get();
    }

    private Investor getZonkyProxy() {
        final AuthenticatedZonky api = Mockito.mock(AuthenticatedZonky.class);
        switch (proxyType) {
            case DRY:
                return new Investor.Builder().asDryRun().build(api);
            case SIMPLE:
                return new Investor.Builder().build(api);
            case CONFIRMING:
                final ConfirmationProvider cp = Mockito.mock(ConfirmationProvider.class);
                Mockito.when(cp.getId()).thenReturn("something");
                switch (confirmationResponse) {
                    case PRESENT:
                        final Confirmation c = (captcha == InvestorTest.Captcha.PROTECTED) ?
                                new Confirmation(ConfirmationType.DELEGATED) :
                                new Confirmation(InvestorTest.CONFIRMED_AMOUNT);
                        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt())).thenReturn(Optional.of(c));
                        break;
                    case ABSENT:
                        Mockito.when(cp.requestConfirmation(ArgumentMatchers.any(), ArgumentMatchers.anyInt(),
                                ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
                        break;
                    default:
                            throw new IllegalStateException();
                }
                return new Investor.Builder().usingConfirmation(cp).build(api);
            default:
                throw new IllegalStateException();
        }
    }

    private void test(final boolean seenBefore) {
        final Recommendation r = this.getRecommendation();
        final Investor p = this.getZonkyProxy();
        ZonkyResponse result;
        try {
            result = p.invest(r, seenBefore);
        } catch (final Exception ex) {
            if (responseType != null) {
                Assertions.fail("Thrown an exception when it shouldn't have.");
            }
            return;
        }
        if (this.proxyType == InvestorTest.ProxyType.CONFIRMING) {
            Assertions.assertThat(p.getConfirmationProviderId()).isPresent();
        } else {
            Assertions.assertThat(p.getConfirmationProviderId()).isEmpty();
        }
        if (responseType == ZonkyResponseType.DELEGATED && seenBefore) {
            Assertions.assertThat(result.getType()).isEqualTo(ZonkyResponseType.SEEN_BEFORE);
        } else {
            Assertions.assertThat(result.getType()).isEqualTo(responseType);
        }
        if (result.getType() == ZonkyResponseType.INVESTED) {
            Assertions.assertThat(result.getConfirmedAmount()).hasValue(InvestorTest.CONFIRMED_AMOUNT);
        } else {
            Assertions.assertThat(result.getConfirmedAmount()).isEmpty();
        }
    }

    @Test
    public void testNeverSeen() {
        test(false);
    }

    private boolean isValidForLoansSeenBefore() {
        // previously delegated means it can still be on the marketplace when checked next time
        return responseType == ZonkyResponseType.DELEGATED;
    }

    @Test
    public void testBeforeSeen() {
        Assume.assumeTrue(this.isValidForLoansSeenBefore());
        test(true);
    }

}
