/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.internal.util.json;

import java.lang.reflect.Type;
import java.util.Objects;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.stream.JsonParser;

import com.github.robozonky.api.remote.enums.MainIncomeType;

public final class MainIncomeTypeDeserializer extends AbstractDeserializer<MainIncomeType> {

    public MainIncomeTypeDeserializer() {
        super(MainIncomeType::valueOf, MainIncomeType.OTHER);
    }

    @Override
    public MainIncomeType deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
        String id = parser.getString();
        if (Objects.equals(id, "OTHERS_MAIN")) { // Don't want to pollute the Enum with this faulty Zonky value.
            return MainIncomeType.OTHER;
        } else {
            return super.deserialize(parser, ctx, rtType);
        }
    }
}
