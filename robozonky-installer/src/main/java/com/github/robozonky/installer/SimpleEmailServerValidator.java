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

package com.github.robozonky.installer;

import com.izforge.izpack.api.data.InstallData;

public class SimpleEmailServerValidator extends AbstractEmailServerValidator {

    @Override
    void configure(final InstallData data) {
        Variables.IS_EMAIL_ENABLED.setValue(data, "true");
        Variables.SMTP_AUTH.setValue(data, "true");
        Variables.SMTP_IS_TLS.setValue(data, "false");
        Variables.SMTP_IS_SSL.setValue(data, "true");
        Variables.SMTP_PORT.setValue(data, "465");
        switch (data.getVariable("emailConfigType")) {
            case "gmail.com":
                Variables.SMTP_HOSTNAME.setValue(data, "smtp.gmail.com");
                return;
            case "seznam.cz":
                Variables.SMTP_HOSTNAME.setValue(data, "smtp.seznam.cz");
                return;
            default:
                throw new IllegalStateException("Can not happen.");
        }
    }
}
