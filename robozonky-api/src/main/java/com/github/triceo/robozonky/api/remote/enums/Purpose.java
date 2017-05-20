/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.remote.enums;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = Purpose.PurposeDeserializer.class)
public enum Purpose {

    AUTO_MOTO, VZDELANI, CESTOVANI, ELEKTRONIKA, ZDRAVI, REFINANCOVANI_PUJCEK, DOMACNOST, VLASTNI_PROJEKT, JINE;

    static class PurposeDeserializer extends JsonDeserializer<Purpose> {

        @Override
        public Purpose deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            final int actualId = Integer.parseInt(id) - 1; // purposes in Zonky API are indexed from 1
            return Purpose.values()[actualId];
        }

    }


}
