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
package org.wso2.carbon.bpmn.rule.task.test;

import org.drools.compiler.compiler.DroolsParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wso2.carbon.bpmn.rule.task.RuleExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule task tests cases.
 */
public class RuleTaskTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Test
    public void testBasicRule() throws IOException, DroolsParserException {
        InputStream inputStream = RuleTaskTest.class.getResourceAsStream("/rule1.drl");
        Map<String, Object> globals = new HashMap<String, Object>();
        List<Object> facts = new ArrayList<Object>();

        globals.put("var1", "Foo");
        globals.put("var2", 100);
        facts.add("Fact 1");
        facts.add("Fact 2");

        RuleExecutor.execute(inputStream, globals, facts);
        String output = outContent.toString();
        StringBuilder expected = new StringBuilder();
        expected.append("Fact: Fact 2" + System.lineSeparator());
        expected.append("var1: Foo" + System.lineSeparator());
        expected.append("var2: 100" + System.lineSeparator());
        expected.append("Fact: Fact 1" + System.lineSeparator());
        expected.append("var1: Foo" + System.lineSeparator());
        expected.append("var2: 100" + System.lineSeparator());
        Assert.assertTrue(output.equals(expected.toString()));
    }

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(null);
        System.setErr(null);
    }
}
