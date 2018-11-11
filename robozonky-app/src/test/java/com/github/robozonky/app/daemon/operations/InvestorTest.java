/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon.operations;

import java.util.stream.Stream;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * There are the following kinds of proxies:
 * <ul>
 * <li>DRY: One in a dry run.</li>
 * <li>SIMPLE: One that has no way of obtaining external investing input.</li>
 * <li>CONFIRMING: One that knows to obtain external investing input.</li>
 * </ul>
 * <p>
 * There are following kinds of investments:
 * <ul>
 * <li>CONFIRMED: Require confirmation, and therefore requiring a CONFIRMING proxy. In case of
 * positive confirmation, we can invest locally.</li>
 * <li>UNCONFIRMED: Not requiring confirmation and not CAPTCHA-protected, these can be invested locally.</li>
 * </ul>
 * <p>
 * Further, investments can be protected by CAPTCHA:
 * <ul>
 * <li>PROTECTED: Can not invest locally, require delegation and therefore a CONFIRMING proxy.</li>
 * <li>UNPROTECTED: Can invest locally, SIMPLE proxy will be enough provided that investment is not CONFIRMED.</li>
 * </ul>
 * <p>
 * Based on that, these are the possible states that need to be tested:
 * <ul>
 * <li>DRY proxy, any investment, regardless of captcha. Always accepted</li>
 * <li>SIMPLE proxy:
 * <ul>
 * <li>Rejects PROTECTED investments since we cannot bypass CAPTCHA.</li>
 * <li>Fails on CONFIRMED investments since we cannot reach confirmation.</li>
 * <li>Fails on network issues.</li>
 * <li>Otherwise invests locally.</li>
 * </ul>
 * </li>
 * <li>CONFIRMING proxy:
 * <ul>
 * <li>Delegates PROTECTED investments.</li>
 * <li>Invests UNPROTECTED CONFIRMED investments locally or rejects them.</li>
 * <li>Invests UNPROTECTED UNCONFIRMED investments locally.</li>
 * <li>Fails on network issues.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * This test aims to test all of these various states.
 */
class InvestorTest extends AbstractZonkyLeveragingTest {

    private static final double LOAN_AMOUNT = 2000.0;
    private static final int CONFIRMED_AMOUNT = (int) (InvestorTest.LOAN_AMOUNT / 2);

    private static LoanDescriptor mockLoanDescriptor(final boolean protectByCaptcha) {
        if (protectByCaptcha) {
            return mockLoanDescriptor();
        } else {
            return mockLoanDescriptorWithoutCaptcha();
        }
    }

    private static RecommendedLoan getRecommendation(final Remote confirmation, final Captcha captcha) {
        final LoanDescriptor ld =
                InvestorTest.mockLoanDescriptor(captcha == InvestorTest.Captcha.PROTECTED);
        return ld.recommend(InvestorTest.CONFIRMED_AMOUNT,
                            confirmation == InvestorTest.Remote.CONFIRMED).get();
    }

    private static boolean isValidForLoansSeenBefore(final ZonkyResponseType responseType) {
        // previously delegated means it can still be on the marketplace when checked next time
        return responseType == ZonkyResponseType.DELEGATED;
    }

    @TestFactory
    Stream<DynamicNode> possibilities() {
        return Stream.of(
                forPossibility(InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.CONFIRMED, null, null),
                forPossibility(InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, null, ZonkyResponseType.REJECTED),
                forPossibility(InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.CONFIRMED, null, null),
                forPossibility(InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, null, ZonkyResponseType.INVESTED),
                // confirming proxy, response positive
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.ACK,
                               ZonkyResponseType.DELEGATED),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.ACK,
                               ZonkyResponseType.DELEGATED),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.ACK,
                               ZonkyResponseType.DELEGATED),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.ACK,
                               ZonkyResponseType.INVESTED),
                // confirming proxy, response negative
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.NAK,
                               ZonkyResponseType.REJECTED),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.NAK,
                               ZonkyResponseType.REJECTED),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.NAK,
                               ZonkyResponseType.REJECTED),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.NAK,
                               ZonkyResponseType.INVESTED)
        );
    }

    private DynamicNode forPossibility(final ProxyType proxyType, final Captcha captcha,
                                       final Remote confirmation, final RemoteResponse confirmationResponse,
                                       final ZonkyResponseType responseType) {
        final RecommendedLoan recommendedLoan = getRecommendation(confirmation, captcha);
        final DynamicTest seenBefore = dynamicTest("seen before",
                                                   () -> testBeforeSeen(proxyType, confirmationResponse, responseType,
                                                                        recommendedLoan));
        final DynamicTest notSeenBefore = dynamicTest("never seen",
                                                      () -> testNeverSeen(proxyType, confirmationResponse,
                                                                          responseType, recommendedLoan));
        final Stream<DynamicTest> tests = isValidForLoansSeenBefore(responseType) ?
                Stream.of(seenBefore, notSeenBefore) : Stream.of(notSeenBefore);
        final String containerName = proxyType + "+" + captcha + "+" + confirmation + "+" + confirmation + "=" +
                responseType;
        return dynamicContainer(containerName, tests);
    }

    private Investor getZonkyProxy(final ProxyType proxyType, final RemoteResponse confirmationResponse,
                                   final Tenant auth) {
        switch (proxyType) {
            case SIMPLE:
                return Investor.build(auth);
            case CONFIRMING:
                final ConfirmationProvider cp = mock(ConfirmationProvider.class);
                when(cp.getId()).thenReturn("something");
                switch (confirmationResponse) {
                    case ACK:
                        when(cp.requestConfirmation(any(), anyInt(),
                                                    anyInt())).thenReturn(true);
                        break;
                    case NAK:
                        when(cp.requestConfirmation(any(), anyInt(),
                                                    anyInt())).thenReturn(false);
                        break;
                    default:
                        throw new IllegalStateException();
                }
                return Investor.build(auth, cp);
            default:
                throw new IllegalStateException();
        }
    }

    private void test(final ProxyType proxyType, final ZonkyResponseType responseType, final RecommendedLoan r,
                      final RemoteResponse confirmationResponse, final boolean seenBefore) {
        final Zonky api = mock(Zonky.class);
        final Investor p = getZonkyProxy(proxyType, confirmationResponse, mockTenant(api, false));
        ZonkyResponse result;
        try {
            result = p.invest(r, seenBefore);
        } catch (final Exception ex) {
            if (responseType != null) {
                fail("Thrown an exception when it shouldn't have.", ex);
            }
            return;
        }
        assertSoftly(softly -> {
            if (proxyType == InvestorTest.ProxyType.CONFIRMING) {
                softly.assertThat(p.getConfirmationProvider()).isPresent();
            } else {
                softly.assertThat(p.getConfirmationProvider()).isEmpty();
            }
            if (responseType == ZonkyResponseType.DELEGATED && seenBefore) {
                softly.assertThat(result.getType()).isEqualTo(ZonkyResponseType.SEEN_BEFORE);
            } else {
                softly.assertThat(result.getType()).isEqualTo(responseType);
            }
            if (result.getType() == ZonkyResponseType.INVESTED) {
                softly.assertThat(result.getConfirmedAmount()).hasValue(InvestorTest.CONFIRMED_AMOUNT);
            } else {
                softly.assertThat(result.getConfirmedAmount()).isEmpty();
            }
        });
        if (responseType == ZonkyResponseType.INVESTED) {
            verify(api).invest(any());
        } else {
            verify(api, never()).invest(any());
        }
    }

    private void testNeverSeen(final ProxyType proxyType, final RemoteResponse confirmationResponse,
                               final ZonkyResponseType responseType, final RecommendedLoan r) {
        test(proxyType, responseType, r, confirmationResponse, false);
    }

    private void testBeforeSeen(final ProxyType proxyType, final RemoteResponse confirmationResponse,
                                final ZonkyResponseType responseType, final RecommendedLoan r) {
        test(proxyType, responseType, r, confirmationResponse, true);
    }

    private enum ProxyType {

        SIMPLE,
        CONFIRMING

    }

    private enum Captcha {

        PROTECTED,
        UNPROTECTED
    }

    private enum Remote {

        CONFIRMED,
        UNCONFIRMED
    }

    private enum RemoteResponse {

        ACK,
        NAK
    }
}
