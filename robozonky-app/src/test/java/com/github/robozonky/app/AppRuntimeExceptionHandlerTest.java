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

package com.github.robozonky.app;

import java.net.SocketException;
import java.net.UnknownHostException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.util.RuntimeExceptionHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

public class AppRuntimeExceptionHandlerTest extends AbstractEventLeveragingTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private final RuntimeExceptionHandler faultTolerant = new AppRuntimeExceptionHandler(true);
    private final RuntimeExceptionHandler regular = new AppRuntimeExceptionHandler(false);

    @Test
    public void unexpectedException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        regular.handle(new IllegalStateException());
    }

    @Test
    public void unexpectedExceptionFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        faultTolerant.handle(new IllegalStateException());
    }

    @Test
    public void unexpectedProcessingException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        regular.handle(new ProcessingException(new IllegalStateException()));
    }

    @Test
    public void unexpectedProcessingExceptionFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_UNEXPECTED.getCode());
        faultTolerant.handle(new ProcessingException(new IllegalStateException()));
    }

    @Test
    public void socketException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        regular.handle(new ProcessingException(new SocketException("Testing exception")));
    }

    @Test
    public void socketExceptionFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        faultTolerant.handle(new ProcessingException(new SocketException("Testing exception")));
    }

    @Test
    public void unknownHostException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        regular.handle(new ProcessingException(new UnknownHostException("Testing exception")));
    }

    @Test
    public void unknownHostExceptionFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        faultTolerant.handle(new ProcessingException(new UnknownHostException("Testing exception")));
    }

    @Test
    public void notAllowedException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        regular.handle(new NotAllowedException("Testing exception"));
    }

    @Test
    public void notAllowedExceptionFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        faultTolerant.handle(new NotAllowedException("Testing exception"));
    }

    @Test
    public void serverErrorException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_DOWN.getCode());
        regular.handle(new ServerErrorException(500));
    }

    @Test
    public void serverErrorExceptionFaultToleratnt() {
        exit.expectSystemExitWithStatus(ReturnCode.OK.getCode());
        faultTolerant.handle(new ServerErrorException(500));
    }

    @Test
    public void webApplicationException() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_REMOTE.getCode());
        regular.handle(new WebApplicationException("Testing exception"));
    }

    @Test
    public void webApplicationExceptionFaultTolerant() {
        exit.expectSystemExitWithStatus(ReturnCode.ERROR_REMOTE.getCode());
        faultTolerant.handle(new WebApplicationException("Testing exception"));
    }
}
