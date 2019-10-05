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

package com.github.robozonky.notifications.samples;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.listeners.AbstractListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public final class Generator {

    private static final Logger LOGGER = LogManager.getLogger(Generator.class);
    private static final File TARGET = new File("output");

    /**
     * This is just a formality. It will not be used.
     */
    private static final AbstractTargetHandler HANDLER = new AbstractTargetHandler(null, null) {
        @Override
        public void send(SessionInfo sessionInfo, String subject, String message, String fallbackMessage) throws Exception {
            throw new UnsupportedOperationException();
        }
    };

    private static final SessionInfo SESSION_INFO = new SessionInfo("info@robozonky.cz", UUID.randomUUID().toString());

    private static <T extends Event> void generateFor(SupportedListener type) {
        final T event = (T) type.getSampleEvent();
        final AbstractListener<T> eventListener = (AbstractListener<T>) type.getListener(HANDLER);
        final Map<String, Object> templateData = new LinkedHashMap<>(eventListener.getData(event, SESSION_INFO));
        templateData.put("subject", eventListener.getSubject(event));
        final String templateFilename = eventListener.getTemplateFileName();
        try {
            final String string = FileBasedTemplateProcessor.INSTANCE.process(templateFilename, templateData);
            final File target = new File(TARGET, templateFilename + ".html");
            LOGGER.info("Storing generated HTML for template {} to {}.", templateFilename, TARGET);
            Files.write(target.toPath(), string.getBytes(Defaults.CHARSET));
        } catch (final Exception ex) {
            LOGGER.error("Failed generating HTML for template {}.", templateFilename, ex);
        }
    }

    public static void main(String... args) throws IOException {
        if (!TARGET.exists()) {
            Files.createDirectory(TARGET.toPath());
        }
        Stream.of(SupportedListener.values()).forEach(Generator::generateFor);
    }

}
