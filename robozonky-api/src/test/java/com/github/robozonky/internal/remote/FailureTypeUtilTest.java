/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class FailureTypeUtilTest {

    @Test
    void differentException() {
        final ClientErrorException actual = new BadRequestException();
        final Class<? extends ClientErrorException> expected = NotFoundException.class;
        assertThat(FailureTypeUtil.matches(expected, actual, null)).isFalse();
    }

    @Test
    void noExpectedReason() {
        final ClientErrorException actual = new BadRequestException();
        final Class<? extends ClientErrorException> expected = actual.getClass();
        assertThat(FailureTypeUtil.matches(expected, actual, null)).isTrue();
    }

    @Test
    void expectedReasonMatches() {
        final String reason = UUID.randomUUID()
            .toString();
        final Response response = Response.status(400)
            .entity(reason)
            .build();
        final ClientErrorException actual = new BadRequestException(response);
        final Class<? extends ClientErrorException> expected = actual.getClass();
        assertThat(FailureTypeUtil.matches(expected, actual, reason)).isTrue();
    }

    @Test
    void expectedReasonDoesNotMatch() {
        final String reason = UUID.randomUUID()
            .toString();
        final Response response = Response.status(400)
            .entity(reason)
            .build();
        final ClientErrorException actual = new BadRequestException(response);
        final Class<? extends ClientErrorException> expected = actual.getClass();
        assertThat(FailureTypeUtil.matches(expected, actual, UUID.randomUUID()
            .toString())).isFalse();
    }

}
