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

package com.github.robozonky.api.remote.entities;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.github.robozonky.api.Ratio;

class RatioAdapter extends XmlAdapter<String, Ratio> {

    @Override
    public Ratio unmarshal(final String s) {
        return Ratio.fromRaw(s);
    }

    @Override
    public String marshal(final Ratio rate) {
        return rate.bigDecimalValue()
            .toPlainString();
    }
}
