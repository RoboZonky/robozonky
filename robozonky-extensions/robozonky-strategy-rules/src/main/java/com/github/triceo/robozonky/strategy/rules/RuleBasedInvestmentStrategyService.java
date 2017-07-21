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

package com.github.triceo.robozonky.strategy.rules;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyService;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.io.KieResources;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses and loads a Drools-based decision table. Supports XLS and XLSX as decision table file formats.
 */
@Deprecated
public class RuleBasedInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedInvestmentStrategyService.class);
    private static final ReleaseId RELEASE_ID = KieServices.Factory.get().getRepository().getDefaultReleaseId();

    @Override
    public Optional<InvestmentStrategy> parse(final InputStream strategy) {
        RuleBasedInvestmentStrategyService.LOGGER.trace("Parsing '{}' started.", strategy);
        final KieServices kieServices = KieServices.Factory.get();
        final KieModuleModel kieModuleModel = kieServices.newKieModuleModel();
        final KieResources resources = kieServices.getResources();
        final KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write("src/main/resources/robozonky.dtable.xls", resources.newInputStreamResource(strategy));
        kfs.writeKModuleXML(kieModuleModel.toXML());
        kfs.generateAndWritePomXML(RuleBasedInvestmentStrategyService.RELEASE_ID);
        final KieBuilder builder = kieServices.newKieBuilder(kfs).buildAll();
        final List<Message> messages = builder.getResults().getMessages(Message.Level.ERROR);
        RuleBasedInvestmentStrategyService.LOGGER.trace("Parsing finished.");
        if (!messages.isEmpty()) {
            throw new IllegalStateException("Failed parsing decision table. Reason: " + messages);
        }
        final KieContainer container = kieServices.newKieContainer(RuleBasedInvestmentStrategyService.RELEASE_ID);
        return Optional.of(new RuleBasedInvestmentStrategy(container));
    }
}
