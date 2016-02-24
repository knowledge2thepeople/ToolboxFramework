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

package org.toolboxframework.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.toolboxframework.Toolbox;
import org.toolboxframework.ToolboxException;
import org.toolboxframework.inject.annotation.UseTool;
import org.toolboxframework.validate.Validate;

/**
 * The ToolInjector class contains the logic to perform tool injection.
 * <p>
 * A typical user of Toolbox will never need to call any of the methods defined
 * in this class explicitly.
 *
 * @author T.C.C.
 * @version 1.0
 * @see org.toolboxframework.inject.annotation.UseTool
 */
public class ToolInjector {

    private ToolInjector() {
    }

    /**
     * Inject tools into all instance fields annotated with
     * {@link org.toolboxframework.inject.annotation.UseTool} belonging to the
     * given object.
     * <p>
     * Under normal circumstances, users of Toolbox would never need to write
     * code that calls this method.
     *
     * @param object
     *            the object into which tools will be injected
     * @exception ToolInjectionException
     *                if unable to inject tools
     * @see org.toolboxframework.inject.annotation.UseTool
     */
    public static void injectTools(final Object object) {
        Validate.argNotNull(object, "object");

        synchronized (Toolbox.class) {
            for (final Field field : object.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(UseTool.class) && !Modifier.isStatic(field.getModifiers())) {
                    injectToolIntoField(object, field);
                }
            }
        }
    }

    private static void injectToolIntoField(final Object object, final Field field) {
        try {
            Object toolValue = null;
            final String nameFromAnnotation = field.getAnnotation(UseTool.class).value();
            if (nameFromAnnotation != null && !nameFromAnnotation.equals("")) {
                // name is forced
                // insist on using a tool with matching name and type
                toolValue = Toolbox.getTool(nameFromAnnotation, field.getType());
            } else {
                // name is not forced
                toolValue = Toolbox.getTool(field.getType(), field.getName());
            }

            field.setAccessible(true);
            field.set(object, toolValue);
        } catch (final ToolboxException e) {
            throw e;
        } catch (final Exception e) {
            throw new ToolInjectionException(String.format("Unable to inject tool into field: %s", field.toString()),
                    e);
        }
    }

    /**
     * Inject tools into all static fields annotated with
     * {@link org.toolboxframework.inject.annotation.UseTool} belonging to the
     * given class.
     * <p>
     * Under normal circumstances, users of Toolbox would never need to write
     * code that calls this method.
     *
     * @param clazz
     *            the class into which tools will be injected
     * @exception ToolInjectionException
     *                if unable to inject tools
     * @see org.toolboxframework.inject.annotation.UseTool
     */
    public static void injectTools(final Class<?> clazz) {
        Validate.argNotNull(clazz, "clazz");

        synchronized (Toolbox.class) {
            for (final Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(UseTool.class) && Modifier.isStatic(field.getModifiers())) {
                    injectToolIntoField(null, field);
                }
            }
        }
    }
}
