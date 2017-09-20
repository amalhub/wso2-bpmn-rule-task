/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.bpmn.rule.task;

import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.compiler.PackageBuilder;
import org.drools.core.RuleBase;
import org.drools.core.RuleBaseFactory;
import org.drools.core.WorkingMemory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * Drools executor
 */
public class RuleExecutor {

    /**
     * Execute drools rules
     * @param inputStream Drools file input stream
     * @param globals Global variables to be injected to the Drools file
     * @param facts Fact to be injected to the Drools file
     * @throws IOException
     * @throws DroolsParserException
     */
    public static void execute(InputStream inputStream, Map<String, Object> globals, List<Object> facts) throws IOException, DroolsParserException {
        PackageBuilder packageBuilder = new PackageBuilder();
        Reader reader = new InputStreamReader(inputStream);
        packageBuilder.addPackageFromDrl(reader);
        org.drools.core.rule.Package rulesPackage = packageBuilder.getPackage();

        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        ruleBase.addPackage(rulesPackage);

        WorkingMemory workingMemory = ruleBase.newStatefulSession();
        for(String key: globals.keySet()) {
            workingMemory.setGlobal(key, globals.get(key));
        }
        for (Object fact: facts) {
            workingMemory.insert(fact);
        }
        workingMemory.fireAllRules();
    }
}
