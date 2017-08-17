/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.triceo.robozonky.api.remote.entities;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.xml.bind.JAXBException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ZonkyApiTokenTest {

    @Test
    public void roundTrip() throws JAXBException {
        final OffsetDateTime obtainedOn = OffsetDateTime.MIN;
        final int expirationInSeconds = 60;
        final ZonkyApiToken token = new ZonkyApiToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                                      expirationInSeconds, obtainedOn);
        final String marshalled = ZonkyApiToken.marshal(token);
        final ZonkyApiToken unmarshalled = ZonkyApiToken.unmarshal(new StringReader(marshalled));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(unmarshalled.getObtainedOn()).isEqualTo(obtainedOn);
            softly.assertThat(unmarshalled.getExpiresOn())
                    .isEqualTo(obtainedOn.plus(expirationInSeconds, ChronoUnit.SECONDS));
        });
    }

    @Test
    public void fresh() {
        final ZonkyApiToken token = new ZonkyApiToken();
        Assertions.assertThat(token.getObtainedOn()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    public void unmarshallWithObtained() throws JAXBException {
        final String datetime = "2017-01-27T11:36:13.413+01:00";
        final String token = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<token>" +
                "<access_token>abc</access_token>" +
                "<refresh_token>def</refresh_token>" +
                "<token_type>refresh_token</token_type>" +
                "<scope>SCOPE_APP_WEB</scope>" +
                "<expires_in>60</expires_in>" +
                "<obtained_on>" + datetime + "</obtained_on>" +
                "</token>";
        final ZonkyApiToken unmarshalled = ZonkyApiToken.unmarshal(new StringReader(token));
        Assertions.assertThat(unmarshalled.getObtainedOn()).isEqualTo(OffsetDateTime.parse(datetime));
    }

    @Test
    public void unmarshallWithoutObtained() throws JAXBException {
        final String token = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<token>" +
                "<access_token>abc</access_token>" +
                "<refresh_token>def</refresh_token>" +
                "<token_type>refresh_token</token_type>" +
                "<scope>SCOPE_APP_WEB</scope>" +
                "<expires_in>60</expires_in>" +
                "</token>";
        final ZonkyApiToken unmarshalled = ZonkyApiToken.unmarshal(new StringReader(token));
        final OffsetDateTime earlyEnough = OffsetDateTime.now().minus(5, ChronoUnit.SECONDS);
        Assertions.assertThat(unmarshalled.getObtainedOn()).isAfter(earlyEnough);
    }
}
