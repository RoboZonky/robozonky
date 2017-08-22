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

package com.github.robozonky.api.remote.entities;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class StatisticsTest {

    @Test
    public void unmarshallNoRiskPortfolio() throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(Statistics.class);
        final Unmarshaller um = jc.createUnmarshaller();
        final Statistics result =
                (Statistics) um.unmarshal(StatisticsTest.class.getResourceAsStream("statistics-no-risk.xml"));
        Assertions.assertThat(result.getRiskPortfolio()).isNotNull(); // null happens if the user never invested yet
    }
}
