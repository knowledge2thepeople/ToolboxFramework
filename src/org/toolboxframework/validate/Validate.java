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

package org.toolboxframework.validate;

import java.util.Collection;

/**
 * The Validate class contains logic for validating input to Toolbox's public
 * APIs.
 *
 * @author T.C.C.
 * @version 1.0
 */
public class Validate {

    /**
     * Validate that a Collection is not null or empty.
     *
     * @param value
     *            the Collection to be validated
     * @param name
     *            the name of the Collection to be validated
     * @exception IllegalArgumentException
     *                if the Collection is null or empty
     */
    public static <T> void argNotEmpty(final Collection<T> value, final String name) {
        if (value == null || value.size() < 1) {
            throw new IllegalArgumentException(String.format("%s cannot be null or empty", name));
        }
    }

    /**
     * Validate that an array is not null or empty.
     *
     * @param value
     *            the array to be validated
     * @param name
     *            the name of the array to be validated
     * @exception IllegalArgumentException
     *                if the array is null or empty
     */
    public static <T> void argNotEmpty(final T[] value, final String name) {
        if (value == null || value.length < 1) {
            throw new IllegalArgumentException(String.format("%s cannot be null or empty", name));
        }
    }

    /**
     * Validate that an Object is not null.
     *
     * @param value
     *            the object to be validated
     * @param name
     *            the name of the object to be validated
     * @exception IllegalArgumentException
     *                if the object is null
     */
    public static void argNotNull(final Object value, final String name) {
        if (value == null) {
            throw new IllegalArgumentException(String.format("%s cannot be null", name));
        }
    }

    /**
     * Validate that a String is not blank.
     *
     * @param value
     *            the String to be validated
     * @param name
     *            the name of the String to be validated
     * @exception IllegalArgumentException
     *                if the String is blank
     */
    public static void argNotBlank(final String value, final String name) {
        if (value == null || value.matches("\\s*")) {
            throw new IllegalArgumentException(String.format("%s cannot be blank", name));
        }
    }

    /**
     * Validate that a String can be used as a tool name.
     * <p>
     * Any non-blank String is acceptable, however, in order to take advantage
     * of the mechanism that falls back to using the name of the field as the
     * tool name when performing field injection and it is not possible to
     * resolve the tool by type, we recommend using names that conform to the
     * restrictions placed on field names.
     *
     * @param value
     *            the tool name to be validated
     * @exception InvalidToolNameException
     *                if the String cannot be used as a tool name
     */
    public static void isValidToolName(final String value) {
        if (value == null || value.matches("\\s*")) {
            throw new InvalidToolNameException(String.format("%s is not a valid tool name", value));
        }
    }

    /**
     * Validate that a String matches the pattern of a Java package.
     *
     * @param value
     *            the String to be validated
     * @param name
     *            the name of the String to be validated
     * @exception IllegalArgumentException
     *                if the String is does not match the pattern of a Java
     *                package
     */
    public static void isJavaPackageName(final String value, final String name) {
        if (value == null || !value.matches("^([\\w\\d]+)(\\.[\\w\\d]+)*$")) {
            throw new IllegalArgumentException(String.format("%s is not a Java package", name));
        }
    }
}
