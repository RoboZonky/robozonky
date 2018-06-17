/*
 * Copyright 2018 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http,//www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.api.remote.enums;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Retrieved from Zonky developers via e-mail on March 27, 2018.
 */
@JsonDeserialize(using = DevelopmentType.CollectionActionTypeDeserializer.class)
public enum DevelopmentType implements BaseEnum {
    LOAN_DELAY_BORROWER(CollectionActionSource.AUTOMATION, "Prodlení", "AUTO_LOAN_DELAY_BORROWER"),
    LOAN_OVERDUE_INST_PENALTY_WARNING(CollectionActionSource.AUTOMATION, "Hrozba pokuty",
                                      "AUTO_LOAN_OVERDUE_INST_PENALTY_WARNING"),
    LOAN_OVERDUE_INST_2ND_INST_APROACHING(CollectionActionSource.AUTOMATION, "Blížící se 2. splátka",
                                          "AUTO_LOAN_OVERDUE_INST_2ND_INST_APROACHING"),
    LOAN_OVERDUE_INST_JUDICIAL_ENFORCMENT_WARNING(CollectionActionSource.AUTOMATION, "Zvažování soudního řešení",
                                                  "AUTO_LOAN_OVERDUE_INST_JUDICIAL_ENFORCMENT_WARNING"),
    LOAN_OVERDUE_INST_2ND_PENALTY_WARNING(CollectionActionSource.AUTOMATION, "Hrozba 2. pokuty",
                                          "AUTO_LOAN_OVERDUE_INST_2ND_PENALTY_WARNING"),
    LOAN_OVERDUE_INST_AFTER_2ND_PENALTY_WARNING(CollectionActionSource.AUTOMATION, "Žádost o neprodlenou úhradu",
                                                "AUTO_LOAN_OVERDUE_INST_AFTER_2ND_PENALTY_WARNING"),
    LOAN_OVERDUE_INST_3RD_INST_APROACHING(CollectionActionSource.AUTOMATION, "Blížící se 3. splátka",
                                          "AUTO_LOAN_OVERDUE_INST_3RD_INST_APROACHING"),
    LOAN_OVERDUE_INST_DEFAULT_WARNING(CollectionActionSource.AUTOMATION, "Hrozba zesplatnění",
                                      "AUTO_LOAN_OVERDUE_INST_DEFAULT_WARNING"),
    LOAN_OVERDUE_INST_3RD_PENALTY_WARNING(CollectionActionSource.AUTOMATION, "Hrozba 3. pokuty",
                                          "AUTO_LOAN_OVERDUE_INST_3RD_PENALTY_WARNING"),
    LOAN_OVERDUE_INSURANCE_POSTPONEMENT_INFO(CollectionActionSource.AUTOMATION, "Možnost odkladu splátky",
                                             "AUTO_LOAN_OVERDUE_INSURANCE_POSTPONEMENT_INFO"),
    LOAN_OVERDUE_INSURANCE_TERMINATION_WARN(CollectionActionSource.AUTOMATION, "Hrozba zániku pojištění",
                                            "AUTO_LOAN_OVERDUE_INSURANCE_TERMINATION_WARN"),
    LOAN_PENALTY_CHANGED(CollectionActionSource.AUTOMATION, "Změna pokuty", "AUTO_LOAN_PENALTY_CHANGED"),
    BORROWER_RECOVERED(CollectionActionSource.AUTOMATION, "Uzdravení klienta", "AUTO_BORROWER_RECOVERED"),
    PAYMENT_BLOCKED(CollectionActionSource.AUTOMATION, "Příchozí transakce", "AUTO_PAYMENT_BLOCKED"),
    PAYMENT_PAIRED(CollectionActionSource.AUTOMATION, "Příchozí transakce", "AUTO_PAYMENT_PAIRED"),
    NOTE(CollectionActionSource.EMPLOYEE, "Poznámka", "EC_NOTE"),
    SUCCESS(CollectionActionSource.EMPLOYEE, "Úspěšný kontakt", "fake.EC_SUCCESS", "EC_SUCCESS_BY_ZONKY",
            "EC_SUCCESS_BY_CLIENT"),
    UNSUCCESS(CollectionActionSource.EMPLOYEE, "Neúspěšný kontakt", "fake.EC_UNSUCCESS", "EC_UNSUCCESS_BY_ZONKY",
              "EC_UNSUCCESS_IMMEDIATE_INTERRUPTED_BY_CLIENT"),
    CREDIT_REDEMPTION_CALL(CollectionActionSource.LEGAL, "Výzva před zesplatněním úvěru",
                           "LC_CREDIT_REDEMPTION_CALL"),
    CREDIT_REDEMPTION(CollectionActionSource.LEGAL, "Zesplatnění úvěru", "LC_CREDIT_REDEMPTION"),
    ZONKY_SENDS_PRETRIAL(CollectionActionSource.LEGAL, "Zonky zasílá předžalobní výzvu",
                         "LC_ZONKY_SENDS_PRETRIAL"),
    LO_SENDS_PRELIGITATION(CollectionActionSource.LEGAL, "AK zaslala předžalobní výzvu",
                           "LC_LO_SENDS_PRELIGITATION"),
    JUDICAL_ENFORCEMENT_INITIATION(CollectionActionSource.LEGAL, "Zahájení soudního vymáhání",
                                   "LC_JUDICAL_ENFORCEMENT_INITIATION"),
    CONCILIATION_APPROVAL(CollectionActionSource.LEGAL, "Schválení smíru", "LC_CONCILIATION_APPROVAL"),
    EXECUTION_LAUNCH(CollectionActionSource.LEGAL, "Zahájení exekuce", "LC_EXECUTION_LAUNCH"),
    INSOLVENCY_PROCEEDINGS_INITIATION(CollectionActionSource.LEGAL, "Zahájení insolvenčního řízení",
                                      "LC_INSOLVENCY_PROCEEDINGS_INITIATION"),
    ALLOWED_DEBT_CREDIT_REDEMPTION(CollectionActionSource.LEGAL, "Povoleno oddlužení – zesplatnění úvěru",
                                   "LC_ALLOWED_DEBT_CREDIT_REDEMPTION"),
    REPAYMEND_SCHEDULE_APPROVING(CollectionActionSource.LEGAL, "Schválení splátkového kalendáře",
                                 "LC_REPAYMEND_SCHEDULE_APPROVING"),
    FILLED_INSOLVENCY_PROPOSAL(CollectionActionSource.LEGAL, "Podali jsme insolvenční návrh",
                               "LC_FILLED_INSOLVENCY_PROPOSAL"),
    INHERITANCE_APPLICATION_SUBMISSION(CollectionActionSource.LEGAL, "Podání přihlášky do dědictví",
                                       "LC_INHERITANCE_APPLICATION_SUBMISSION"),
    PARTIAL_SATISFACTION_OF_CLAIM_IN_THE_INHERITANCE(CollectionActionSource.LEGAL,
                                                     "Částečné uspokojení pohledávky v dědictví",
                                                     "LC_PARTIAL_SATISFACTION_OF_CLAIM_IN_THE_INHERITANCE"),
    FULL_SATISFACTION_OF_CLAIM_IN_THE_INHERITANCE(CollectionActionSource.LEGAL,
                                                  "Úplné uspokojení pohledávky v dědictví",
                                                  "LC_FULL_SATISFACTION_OF_CLAIM_IN_THE_INHERITANCE"),
    UNSATISFACTORY_CLAIMS_IN_THE_INHERITANCE(CollectionActionSource.LEGAL, "Neuspokojení pohledávky v dědictví",
                                             "LC_UNSATISFACTORY_CLAIMS_IN_THE_INHERITANCE"),
    INFRINGEMENT_OF_AGREEMENT_APPEARED(CollectionActionSource.LEGAL, "Zonky se dozví o porušení smlouvy",
                                       "LC_INFRINGEMENT_OF_AGREEMENT_APPEARED"),
    FILED_CRIMINAL_NOTICE(CollectionActionSource.LEGAL, "Podáno trestní oznámení", "LC_FILED_CRIMINAL_NOTICE"),
    PCR_DECISION(CollectionActionSource.LEGAL, "Rozhodnutí PČR", "LC_PCR_DECISION"),
    REJECTED_CRIMINAL_NOTICE_BY_PCR(CollectionActionSource.LEGAL, "Rozhodnutí PČR o zamítnutí TO",
                                    "LC_REJECTED_CRIMINAL_NOTICE_BY_PCR"),
    SECOND_CHANCE(CollectionActionSource.LEGAL, "Druhá šance", "LC_SECOND_CHANCE"),
    COURT_SETTLEMENT(CollectionActionSource.LEGAL, "Soudní smír", "LC_COURT_SETTLEMENT"),
    OTHER(CollectionActionSource.LEGAL, "*** Jiné ***", "LC_OTHER");

    private final CollectionActionSource source;
    private final String code;
    private final Set<String> ids;

    DevelopmentType(final CollectionActionSource source, final String code, final String... internalId) {
        this.source = source;
        this.code = code;
        this.ids = Collections.unmodifiableSet(Stream.of(internalId).collect(Collectors.toSet()));
    }

    public CollectionActionSource getSource() {
        return source;
    }

    @Override
    public String getCode() {
        return code;
    }

    public Set<String> getIds() {
        return ids;
    }

    static class CollectionActionTypeDeserializer extends JsonDeserializer<DevelopmentType> {

        @Override
        public DevelopmentType deserialize(final JsonParser jsonParser,
                                           final DeserializationContext ctxt) throws IOException {
            final String id = jsonParser.getText();
            return Stream.of(DevelopmentType.values())
                    .filter(t -> t.getIds().contains(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unknown enum value: " + id));
        }
    }
}

