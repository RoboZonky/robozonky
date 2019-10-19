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

@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(type = char[].class, value = CharArrayAdapter.class),
        @XmlJavaTypeAdapter(type = OffsetDateTime.class, value = OffsetDateTimeAdapter.class),
        @XmlJavaTypeAdapter(type = LocalDate.class, value = LocalDateAdapter.class),
        @XmlJavaTypeAdapter(type = YearMonth.class, value = YearMonthAdapter.class),
        @XmlJavaTypeAdapter(type = OAuthScopes.class, value = OAuthScopeAdapter.class),
        @XmlJavaTypeAdapter(type = Ratio.class, value = RatioAdapter.class)
})
package com.github.robozonky.api.remote.entities;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.OAuthScopes;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
