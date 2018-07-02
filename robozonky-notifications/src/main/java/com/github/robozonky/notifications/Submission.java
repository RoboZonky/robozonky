/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.notifications;

import java.io.IOException;
import java.util.Map;

import com.github.robozonky.api.SessionInfo;
import freemarker.template.TemplateException;

/**
 * The point of this interface is to serve as the data transfer vehicle between the listener and the handler. All the
 * heavy lifting, such as message creation from the templates, will only happen in the handler, when we're 100 % sure
 * that the message will be sent and therefore the processing effort won't be in vain.
 */
public interface Submission {

    SessionInfo getSessionInfo();

    SupportedListener getSupportedListener();

    Map<String, Object> getData();

    String getSubject();

    String getMessage(final Map<String, Object> data) throws IOException, TemplateException;

    String getFallbackMessage(final Map<String, Object> data) throws IOException, TemplateException;
}
