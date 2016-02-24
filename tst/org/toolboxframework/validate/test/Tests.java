/*
 * Copyright (C) 2015 the original author.
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

package org.toolboxframework.validate.test;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.toolboxframework.Toolbox;
import org.toolboxframework.scan.ToolScanner;
import org.toolboxframework.validate.Validate;

public class Tests {
    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUpSuite() {
        final Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);

        final ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS z");
        final Formatter formatter = new Formatter() {
            @Override
            public String format(final LogRecord record) {
                return String.format("%s%n%s%n%n", dateFormat.format(new Date(record.getMillis())),
                        MessageFormat.format(record.getMessage(), record.getParameters()));
            }
        };
        handler.setFormatter(formatter);
    }

    @Before
    public void setupTest() {
        ToolScanner.clearScanHistory();
        Toolbox.clear();
        System.out.printf("%n%s%n%n", name.getMethodName());
    }

    @After
    public void tearDown() {
        System.out.println();
    }

    @Test
    public void testToolNameStartingWithDollarSign() {
        Validate.isValidToolName("$adfsd");
    }
}
