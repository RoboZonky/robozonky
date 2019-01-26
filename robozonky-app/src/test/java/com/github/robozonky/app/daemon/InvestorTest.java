/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import io.vavr.control.Either;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

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
    private static final BigDecimal CONFIRMED_AMOUNT_BIG = BigDecimal.valueOf(CONFIRMED_AMOUNT);

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

    private static boolean isValidForLoansSeenBefore(final Either<InvestmentFailure, BigDecimal> responseType) {
        // previously delegated means it can still be on the marketplace when checked next time
        if (responseType == null) {
            return false;
        }
        return responseType.fold(response -> response == InvestmentFailure.DELEGATED, amount -> false);
    }

    @TestFactory
    Stream<DynamicNode> possibilities() {
        return Stream.of(
                forPossibility(InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, null,
                               Either.left(InvestmentFailure.REJECTED)),
                forPossibility(InvestorTest.ProxyType.SIMPLE, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, null,
                               Either.right(InvestorTest.CONFIRMED_AMOUNT_BIG)),
                // confirming proxy, response positive
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.ACK,
                               Either.left(InvestmentFailure.DELEGATED)),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.ACK,
                               Either.left(InvestmentFailure.DELEGATED)),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.ACK,
                               Either.left(InvestmentFailure.DELEGATED)),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.ACK,
                               Either.right(InvestorTest.CONFIRMED_AMOUNT_BIG)),
                // confirming proxy, response negative
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.NAK,
                               Either.left(InvestmentFailure.REJECTED)),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.PROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.NAK,
                               Either.left(InvestmentFailure.REJECTED)),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.CONFIRMED, InvestorTest.RemoteResponse.NAK,
                               Either.left(InvestmentFailure.REJECTED)),
                forPossibility(InvestorTest.ProxyType.CONFIRMING, InvestorTest.Captcha.UNPROTECTED,
                               InvestorTest.Remote.UNCONFIRMED, InvestorTest.RemoteResponse.NAK,
                               Either.right(InvestorTest.CONFIRMED_AMOUNT_BIG))
        );
    }

    private DynamicNode forPossibility(final ProxyType proxyType, final Captcha captcha,
                                       final Remote confirmation, final RemoteResponse confirmationResponse,
                                       final Either<InvestmentFailure, BigDecimal> expectedResponse) {
        final RecommendedLoan recommendedLoan = getRecommendation(confirmation, captcha);
        final DynamicTest seenBefore = dynamicTest("seen before",
                                                   () -> testBeforeSeen(proxyType, confirmationResponse,
                                                                        expectedResponse, recommendedLoan));
        final DynamicTest notSeenBefore = dynamicTest("never seen",
                                                      () -> testNeverSeen(proxyType, confirmationResponse,
                                                                          expectedResponse, recommendedLoan));
        final Stream<DynamicTest> tests = isValidForLoansSeenBefore(expectedResponse) ?
                Stream.of(seenBefore, notSeenBefore) : Stream.of(notSeenBefore);
        final String containerName = proxyType + "+" + captcha + "+" + confirmation + "=" + expectedResponse;
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

    private void test(final ProxyType proxyType, final Either<InvestmentFailure, BigDecimal> responseType,
                      final RecommendedLoan r, final RemoteResponse confirmationResponse, final boolean seenBefore) {
        final Zonky api = harmlessZonky(10_000);
        final Investor p = getZonkyProxy(proxyType, confirmationResponse, mockTenant(api, false));
        final Either<InvestmentFailure, BigDecimal> result = p.invest(r, seenBefore);
        assertSoftly(softly -> {
            if (proxyType == InvestorTest.ProxyType.CONFIRMING) {
                softly.assertThat(p.getConfirmationProvider()).isPresent();
            } else {
                softly.assertThat(p.getConfirmationProvider()).isEmpty();
            }
            if (responseType.isRight()) {
                verify(api).invest(any());
                softly.assertThat(result.get()).isEqualTo(InvestorTest.CONFIRMED_AMOUNT_BIG);
            } else {
                verify(api, never()).invest(any());
                if (responseType.getLeft() == InvestmentFailure.DELEGATED && seenBefore) {
                    softly.assertThat(result.getLeft()).isEqualTo(InvestmentFailure.SEEN_BEFORE);
                } else {
                    softly.assertThat(result.getLeft()).isEqualTo(responseType.getLeft());
                }
            }
        });
    }

    private void testNeverSeen(final ProxyType proxyType, final RemoteResponse confirmationResponse,
                               final Either<InvestmentFailure, BigDecimal> responseType, final RecommendedLoan r) {
        test(proxyType, responseType, r, confirmationResponse, false);
    }

    private void testBeforeSeen(final ProxyType proxyType, final RemoteResponse confirmationResponse,
                                final Either<InvestmentFailure, BigDecimal> responseType, final RecommendedLoan r) {
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
