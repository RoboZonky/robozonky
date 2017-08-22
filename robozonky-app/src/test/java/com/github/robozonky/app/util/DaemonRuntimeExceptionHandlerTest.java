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

package com.github.robozonky.app.util;

import java.net.SocketException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;

import com.github.robozonky.api.notifications.RemoteOperationFailedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.AbstractEventsAndStateLeveragingTest;
import com.github.robozonky.app.Events;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DaemonRuntimeExceptionHandlerTest extends AbstractEventsAndStateLeveragingTest {

    private final RuntimeExceptionHandler handler = new DaemonRuntimeExceptionHandler();

    @Test
    public void randomException() {
        handler.handle(new IllegalStateException());
        Assertions.assertThat(Events.getFired()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    public void serverErrorException() {
        handler.handle(new ServerErrorException(500));
        Assertions.assertThat(Events.getFired()).first().isInstanceOf(RemoteOperationFailedEvent.class);
    }

    @Test
    public void processingException() {
        handler.handle(new ProcessingException(new SocketException("Testing exception")));
        Assertions.assertThat(Events.getFired()).first().isInstanceOf(RemoteOperationFailedEvent.class);
    }

    @Test
    public void notAllowedException() {
        handler.handle(new NotAllowedException("Testing exception"));
        Assertions.assertThat(Events.getFired()).first().isInstanceOf(RemoteOperationFailedEvent.class);
    }

    @Test
    public void webApplicationException() {
        handler.handle(new WebApplicationException("Testing exception"));
        Assertions.assertThat(Events.getFired()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }
}
