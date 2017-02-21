/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.io.IOException;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.internal.api.Defaults;
import freemarker.template.TemplateException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TemplateProcessorTest extends AbstractEmailingListenerTest {

    @Test
    public void processingWithoutErrors() throws IOException, TemplateException {
        final AbstractEmailingListener<Event> l = this.getEmailingListener();
        final String s = TemplateProcessor.INSTANCE.process(l.getTemplateFileName(), l.getData(event));
        Assertions.assertThat(s).contains(Defaults.ROBOZONKY_URL);
    }

}
