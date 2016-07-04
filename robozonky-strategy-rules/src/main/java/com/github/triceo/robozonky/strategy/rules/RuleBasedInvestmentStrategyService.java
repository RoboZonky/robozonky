/*
 * Copyright 2016 Lukáš Petrovický
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

import java.io.File;
import java.util.List;

import com.github.triceo.robozonky.Util;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import com.github.triceo.robozonky.strategy.InvestmentStrategyService;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.io.KieResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleBasedInvestmentStrategyService implements InvestmentStrategyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleBasedInvestmentStrategyService.class);
    private static final ReleaseId RELID = KieServices.Factory.get().newReleaseId("com.github.triceo.robozonky",
            "rule-based-strategy", Util.getRoboZonkyVersion());

    @Override
    public InvestmentStrategy parse(final File strategyFile) throws InvestmentStrategyParseException {
        RuleBasedInvestmentStrategyService.LOGGER.trace("Parsing '{}' started.", strategyFile);
        final KieServices kieServices = KieServices.Factory.get();
        final KieModuleModel kieModuleModel = kieServices.newKieModuleModel();
        final KieResources resources = kieServices.getResources();
        final KieFileSystem kfs = kieServices.newKieFileSystem();
        kfs.write(resources.newFileSystemResource(strategyFile));
        kfs.writeKModuleXML(kieModuleModel.toXML());
        kfs.generateAndWritePomXML(RuleBasedInvestmentStrategyService.RELID);
        final KieBuilder builder = kieServices.newKieBuilder(kfs).buildAll();
        final List<Message> messages = builder.getResults().getMessages(Message.Level.ERROR);
        RuleBasedInvestmentStrategyService.LOGGER.trace("Parsing finished.");
        if (!messages.isEmpty()) {
            throw new InvestmentStrategyParseException("Failed parsing decision table. Reason: " + messages);
        }
        return new RuleBasedInvestmentStrategy(kieServices.newKieContainer(RuleBasedInvestmentStrategyService.RELID));
    }

    @Override
    public boolean isSupported(final File strategyFile) {
        return strategyFile.getAbsolutePath().endsWith(".xls") || strategyFile.getAbsolutePath().endsWith(".xlsx");
    }

}
