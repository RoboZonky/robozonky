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

package com.github.robozonky.internal.remote;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.assertj.core.api.SoftAssertions;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.internal.remote.InvestmentFailureType.INSUFFICIENT_BALANCE;
import static com.github.robozonky.internal.remote.InvestmentFailureType.UNKNOWN;
import static org.assertj.core.api.Assertions.*;

class InvestmentResultTest {

    @Test
    void success() {
        final InvestmentResult result = InvestmentResult.success();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.isSuccess()).isTrue();
            softly.assertThat(result.getFailureType()).isEmpty();
            softly.assertThat(result).isEqualTo(result);
            softly.assertThat(result).isSameAs(InvestmentResult.success());
        });
    }

    @Test
    void sameCauseEquals() {
        assertThat(InvestmentResult.failure(new BadRequestException())).isEqualTo(InvestmentResult.failure(new BadRequestException()));
    }

    @Test
    void nullException() {
        assertThat(InvestmentResult.failure(null))
                .matches(r -> !r.isSuccess())
                .matches(r -> r.getFailureType().isPresent() && r.getFailureType().get() == UNKNOWN);
    }

    @Test
    void unknownException() {
        assertThat(InvestmentResult.failure(new ClientErrorException(410)))
                .matches(r -> !r.isSuccess())
                .matches(r -> r.getFailureType().isPresent() && r.getFailureType().get() == UNKNOWN);
    }

    @Test
    void noReason() {
        final Response response = new ResponseBuilderImpl()
                .status(400)
                .build();
        final ClientErrorException ex = new BadRequestException(response);
        assertThat(InvestmentResult.failure(ex))
                .matches(r -> !r.isSuccess())
                .matches(r -> r.getFailureType().isPresent() && r.getFailureType().get() == UNKNOWN);
    }

    @Test
    void insufficientBalance() {
        final Response response = new ResponseBuilderImpl()
                .status(400)
                .entity(INSUFFICIENT_BALANCE.getReason().get())
                .build();
        final ClientErrorException ex = new BadRequestException(response);
        assertThat(InvestmentResult.failure(ex))
                .matches(r -> !r.isSuccess())
                .matches(r -> r.getFailureType().isPresent() && r.getFailureType().get() == INSUFFICIENT_BALANCE);
    }
}
