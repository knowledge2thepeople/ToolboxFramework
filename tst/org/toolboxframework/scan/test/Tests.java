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

package org.toolboxframework.scan.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.toolboxframework.NoUniqueToolForTypeException;
import org.toolboxframework.ToolNotFoundException;
import org.toolboxframework.ToolNotOfRequiredTypeException;
import org.toolboxframework.Toolbox;
import org.toolboxframework.scan.CircularDependencyException;
import org.toolboxframework.scan.MethodAnnotatedWithToolReturnsVoidException;
import org.toolboxframework.scan.ToolScanner;

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

    private void printFirstFewLinesOfStackTrace(final Exception e) {
        System.out.printf("%s%n", e.getClass().getName());
        System.out.printf("%s%n", e.getMessage() == null ? "null" : e.getMessage());

        final StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < 2 && i < stackTrace.length; ++i) {
            System.out.println(stackTrace[i].toString());
        }
    }

    @Test(expected = MethodAnnotatedWithToolReturnsVoidException.class)
    public void testMethodReturnsVoidToolMaker() {
        try {
            ToolScanner.scanClass(MethodReturnsVoidToolMaker.class);
        } catch (final MethodAnnotatedWithToolReturnsVoidException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = CircularDependencyException.class)
    public void testToolDependsOnToolWithSameNameAndTypeToolMaker() {
        try {
            ToolScanner.scanClass(ToolDependsOnToolWithSameNameAndTypeToolMaker.class);
        } catch (final CircularDependencyException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = CircularDependencyException.class)
    public void testToolDependsOnToolWithSameNameAndDifferentTypeToolMaker() {
        try {
            ToolScanner.scanClass(ToolDependsOnToolWithSameNameAndDifferentTypeToolMaker.class);
        } catch (final CircularDependencyException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = CircularDependencyException.class)
    public void testCircularDependencyWith3ToolsToolMaker() {
        try {
            ToolScanner.scanClass(CircularDependencyWith3ToolsToolMaker.class);
        } catch (final CircularDependencyException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = ToolNotOfRequiredTypeException.class)
    public void testToolDeclaresDependencyUsingExplicitNameOfExistingToolButWrongTypeToolMaker() {
        Toolbox.addTool("$#@!", "sun");
        try {
            ToolScanner.scanClass(ToolDeclaresDependencyUsingExplicitNameOfExistingToolButWrongTypeToolMaker.class);
        } catch (final ToolNotOfRequiredTypeException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = NoUniqueToolForTypeException.class)
    public void testToolDependsOnSameTypeOfUniqueToolOfTypeInToolBoxWithoutForcingNameToolMaker() {
        Toolbox.addTool("nail", "nail");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnSameTypeOfUniqueToolOfTypeInToolBoxWithoutForcingNameToolMaker.class);
        } catch (final NoUniqueToolForTypeException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test
    public void testToolDependsOnUniqueToolOfTypeInToolBoxWithForcingNameToolMaker() {
        Toolbox.addTool("nail", "nail");
        ToolScanner
                .scanClass(ToolDependsOnUniqueToolOfTypeInToolBoxWithForcingNameToolMaker.class);
    }

    @Test(expected = ToolNotFoundException.class)
    public void testToolDependsOnUniqueToolOfTypeInToolBoxWithForcingIncorrectNameToolMaker() {
        Toolbox.addTool("nail", "nail");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnUniqueToolOfTypeInToolBoxWithForcingIncorrectNameToolMaker.class);
        } catch (final ToolNotFoundException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = NoUniqueToolForTypeException.class)
    public void testToolDependsOnNonUniqueToolOfTypeInToolBoxWithoutForcingNameToolMaker() {
        Toolbox.addTool("nail", "nail");
        Toolbox.addTool("screw", "screw");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnNonUniqueToolOfTypeInToolBoxWithoutForcingNameToolMaker.class);
        } catch (final NoUniqueToolForTypeException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test
    public void testToolDependsOnNonUniqueToolOfTypeInToolBoxWithForcingNameToolMaker() {
        Toolbox.addTool("nail", "nail");
        Toolbox.addTool("screw", "screw");
        ToolScanner.scanClass(
                ToolDependsOnNonUniqueToolOfTypeInToolBoxWithForcingNameToolMaker.class);
    }

    @Test(expected = ToolNotOfRequiredTypeException.class)
    public void testToolDependsOnUniqueToolOfTypeInToolBoxWithForcingNameOfAnotherToolOfAnotherTypeToolMaker() {
        Toolbox.addTool(false, "flag");
        Toolbox.addTool(10L, "score");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnUniqueToolOfTypeInToolBoxWithForcingNameOfAnotherToolOfAnotherTypeToolMaker.class);
        } catch (final ToolNotOfRequiredTypeException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = ToolNotOfRequiredTypeException.class)
    public void testToolDependsOnUniqueToolOfTypeInToolBoxWithForcingNameOfAnotherToolOfAnotherTypeToolClass() {
        Toolbox.addTool(false, "flag");
        Toolbox.addTool(10L, "score");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnUniqueToolOfTypeInToolBoxWithForcingNameOfAnotherToolOfAnotherTypeToolClass.class);
        } catch (final ToolNotOfRequiredTypeException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test
    public void testToolDependsOnUniqueToolOfTypeInToolBoxWithWrongFieldNameWithoutForcingNameToolClass() {
        Toolbox.addTool(true, "flag");
        Toolbox.addTool(10L, "score");
        ToolScanner.scanClass(
                ToolDependsOnUniqueToolOfTypeInToolBoxWithWrongFieldNameWithoutForcingNameToolClass.class);
        final ToolDependsOnUniqueToolOfTypeInToolBoxWithWrongFieldNameWithoutForcingNameToolClass tool = Toolbox
                .getTool(ToolDependsOnUniqueToolOfTypeInToolBoxWithWrongFieldNameWithoutForcingNameToolClass.class);
        assertTrue(tool.member);
        assertTrue(tool.member2 == 10L);
    }

    @Test(expected = NoUniqueToolForTypeException.class)
    public void testToolDependsOnNonUniqueToolOfTypeInToolBoxWithImplicitNameOfAnotherToolOfAnotherTypeToolClass() {
        Toolbox.addTool(true, "switch");
        Toolbox.addTool(true, "flag");
        Toolbox.addTool(10L, "score");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnNonUniqueToolOfTypeInToolBoxWithImplicitNameOfAnotherToolOfAnotherTypeToolClass.class);
        } catch (final NoUniqueToolForTypeException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test(expected = ToolNotFoundException.class)
    public void testToolDependsOnNonExistentToolOfTypeInToolBoxWithImplicitNameOfAnotherToolOfAnotherTypeToolClass() {
        Toolbox.addTool(10L, "score");
        try {
            ToolScanner.scanClass(
                    ToolDependsOnNonExistentToolOfTypeInToolBoxWithImplicitNameOfAnotherToolOfAnotherTypeToolClass.class);
        } catch (final ToolNotFoundException e) {
            printFirstFewLinesOfStackTrace(e);
            throw e;
        }
        fail("Test finished unexpectedly");
    }

    @Test
    public void testAddingAndGettingPrimitive() {
        Toolbox.addTool(false, "flag");
        Toolbox.getTool("flag", boolean.class);
    }

    @Test
    public void testAddingPrimitiveAndGettingObject() {
        Toolbox.addTool(false, "flag");
        Toolbox.getTool("flag", Boolean.class);
    }

    @Test
    public void testStaticMethodsDontDependOnInstanceFieldsToolMaker() {
        ToolScanner.scanClasses(
                new Class<?>[] { A.class, B.class, C.class, StaticMethodsDontDependOnInstanceFieldsToolMaker.class });
    }

    @Test
    public void testToolDependsOnADependencyOfADependencyToolMaker() {
        ToolScanner.scanClass(ToolDependsOnADependencyOfADependencyToolMaker.class);
    }
}
