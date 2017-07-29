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

package com.github.triceo.robozonky.installer.panels;

import com.izforge.izpack.api.data.InstallData;

enum Variables {

    EMAIL_IS_LOAN_DELINQUENT_10_PLUS("isLoanDelinquent10PlusEnabled"),
    EMAIL_IS_LOAN_DELINQUENT_30_PLUS("isLoanDelinquent30PlusEnabled"),
    EMAIL_IS_LOAN_DELINQUENT_60_PLUS("isLoanDelinquent60PlusEnabled"),
    EMAIL_IS_LOAN_DELINQUENT_90_PLUS("isLoanDelinquent90PlusEnabled"),
    EMAIL_IS_LOAN_NOT_DELINQUENT("isLoanNoLongerDelinquentEnabled"),
    EMAIL_IS_INVESTMENT("isInvestmentNotificationEnabled"),
    EMAIL_IS_BALANCE_OVER_200("isBalanceOver200NotificationEnabled"),
    EMAIL_IS_FAILURE("isFailureNotificationEnabled"),
    EMAIL_IS_CRITICAL_FAILURE("isCriticalFailureNotificationEnabled"),
    INSTALL_PATH("INSTALL_PATH"),
    IS_DRY_RUN("isDryRun"),
    IS_EMAIL_ENABLED("isEmailEnabled"),
    IS_JMX_ENABLED("isJmxEnabled"),
    IS_JMX_SECURITY_ENABLED("isJmxSecurityEnabled"),
    IS_USING_OAUTH_TOKEN("isUsingToken"),
    IS_WINDOWS("isWindowsInstall"),
    IS_ZONKOID_ENABLED("isZonkoidEnabled"),
    JAVA_HOME("JAVA_HOME"),
    SMTP_TO("smtpTo"),
    SMTP_USERNAME("smtpUsername"),
    SMTP_PASSWORD("smtpPassword"),
    SMTP_HOSTNAME("smtpHostname"),
    SMTP_PORT("smtpPort"),
    SMTP_IS_TLS("smtpIsTls"),
    SMTP_IS_SSL("smtpIsSsl"),
    JMX_HOSTNAME("jmxHostname"),
    JMX_PORT("jmxPort"),
    STRATEGY_SOURCE("strategy"),
    STRATEGY_TYPE("strategyType"),
    ZONKOID_TOKEN("zonkoidToken"),
    ZONKY_USERNAME("zonkyUsername"),
    ZONKY_PASSWORD("zonkyPassword");

    private final String key;

    Variables(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getValue(final InstallData installData) {
        return installData.getVariable(key);
    }

}
