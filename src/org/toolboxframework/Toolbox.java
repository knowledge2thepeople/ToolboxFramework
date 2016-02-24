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

package org.toolboxframework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.toolboxframework.validate.Validate;

/**
 * The Toolbox class provides ways to add and remove tools from the toolbox, as
 * well as to know whether or not the toolbox contains a tool.
 * <p>
 * Typical usage could include using the addTool({@link java.lang.Object},
 * {@link java.lang.String}) method to add tools to the toolbox before
 * constructing instances of classes which use the
 * {@link org.toolboxframework.inject.annotation.UseTool} field annotation.
 * However, scanning for tools using one of the methods provided in
 * {@link org.toolboxframework.scan.ToolScanner} is usually a more convenient
 * way to populate the toolbox with tools. Therefore, there is rarely ever a
 * need to write code that makes calls to the methods in this class.
 *
 * @author T.C.C.
 * @version 1.0
 * @see org.toolboxframework.scan.ToolScanner
 */
public class Toolbox {
    private final static Map<Class<?>, List<String>> TOOL_NAMES_BY_CLASS = new HashMap<Class<?>, List<String>>();
    private final static Map<String, Object> TOOLS_BY_NAME = new HashMap<String, Object>();
    private final static Map<Class<?>, Class<?>> OBJECT_TYPES_TO_PRIMITIVE_TYPES = new HashMap<Class<?>, Class<?>>();

    static {
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Boolean.class, boolean.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Byte.class, byte.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Character.class, char.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Double.class, double.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Float.class, float.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Integer.class, int.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Long.class, long.class);
        OBJECT_TYPES_TO_PRIMITIVE_TYPES.put(Short.class, short.class);
    }

    private final static Map<Class<?>, Class<?>> PRIMITIVE_TYPES_TO_OBJECT_TYPES = new HashMap<Class<?>, Class<?>>();

    static {
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(boolean.class, Boolean.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(byte.class, Byte.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(char.class, Character.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(double.class, Double.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(float.class, Float.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(int.class, Integer.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(long.class, Long.class);
        PRIMITIVE_TYPES_TO_OBJECT_TYPES.put(short.class, Short.class);
    }

    private Toolbox() {
    }

    /**
     * Add the given object to the toolbox, using the given name as the object's
     * identifier.
     *
     * @param tool
     *            the object to put in the toolbox
     * @param toolName
     *            the name to use as the object's identifier (i.e. tool name)
     * @exception org.toolboxframework.ToolNameAlreadyInUseException
     *                if the toolbox already contains a tool with the given name
     */
    public static synchronized void addTool(final Object tool, final String toolName) {
        Validate.argNotNull(tool, "tool");
        Validate.argNotBlank(toolName, "toolName");
        Validate.isValidToolName(toolName);

        if (containsTool(toolName)) {
            throw new ToolNameAlreadyInUseException(String.format("tool name: %s", toolName));
        }

        TOOLS_BY_NAME.put(toolName, tool);
        associateToolWithAllApplicableClasses(tool, toolName);
    }

    private static synchronized void associateToolWithAllApplicableClasses(final Object tool, final String toolName) {
        final Set<Class<?>> assignableClasses = getAllAssignableClasses(tool);
        for (final Class<?> clazz : assignableClasses) {
            if (TOOL_NAMES_BY_CLASS.containsKey(clazz) && TOOL_NAMES_BY_CLASS.get(clazz) != null) {
                TOOL_NAMES_BY_CLASS.get(clazz).add(toolName);
            } else {
                final List<String> toolNames = new ArrayList<String>();
                toolNames.add(toolName);
                TOOL_NAMES_BY_CLASS.put(clazz, toolNames);
            }
        }
    }

    private static Set<Class<?>> getAllAssignableClasses(final Object object) {
        final Class<?> objectClass = object.getClass();
        final Set<Class<?>> assignableClasses = new HashSet<Class<?>>();

        final Queue<Class<?>> queue = new LinkedList<Class<?>>();
        queue.add(objectClass);

        while (!queue.isEmpty()) {
            final Class<?> currentClass = queue.poll();
            if (currentClass == null) {
                continue;
            }
            assignableClasses.add(currentClass);
            if (OBJECT_TYPES_TO_PRIMITIVE_TYPES.containsKey(currentClass)) {
                assignableClasses.add(OBJECT_TYPES_TO_PRIMITIVE_TYPES.get(currentClass));
            }
            Collections.addAll(queue, currentClass.getInterfaces());
            queue.add(currentClass.getSuperclass());
        }

        return assignableClasses;
    }

    /**
     * Remove the tool with the given name and type from the toolbox.
     *
     * @param toolName
     *            the name of the tool to remove
     * @param requiredType
     *            type the tool must match (can be an interface or superclass of
     *            the actual class)
     * @exception org.toolboxframework.ToolNotFoundException
     *                if the toolbox does not contain a tool with the given name
     * @exception org.toolboxframework.ToolNotOfRequiredTypeException
     *                if the tool is not of the required type
     */
    public static synchronized void removeTool(final String toolName, final Class<?> requiredType) {
        Validate.argNotBlank(toolName, "toolName");
        Validate.argNotNull(requiredType, "requiredType");

        if (!containsTool(toolName)) {
            throw new ToolNotFoundException(String.format("tool name: %s", toolName));
        }

        final Object tool = TOOLS_BY_NAME.get(toolName);
        if (!requiredType.isAssignableFrom(tool.getClass())) {
            throw new ToolNotOfRequiredTypeException(String.format("tool name: %s, required type: %s, actual type: %s",
                    toolName, requiredType.getName(), tool.getClass().getName()));
        }

        TOOLS_BY_NAME.remove(toolName);
        disassociateToolWithAllApplicableClasses(tool, toolName);
    }

    private static void disassociateToolWithAllApplicableClasses(final Object tool, final String toolName) {
        final Set<Class<?>> assignableClasses = getAllAssignableClasses(tool);
        for (final Class<?> clazz : assignableClasses) {
            if (TOOL_NAMES_BY_CLASS.get(clazz) != null
                    && TOOL_NAMES_BY_CLASS.get(clazz).contains(toolName)) {
                TOOL_NAMES_BY_CLASS.get(clazz).remove(toolName);
            }
        }
    }

    /**
     * Return the tool that uniquely matches the given object type, or, if there
     * are multiple tools in the toolbox that match the given object type,
     * return the one with the given fallback name.
     *
     * @param requiredType
     *            type the tool must match (can be an interface or superclass of
     *            the actual class)
     * @param fallbackName
     *            the tool name to fallback on in case the tool cannot be
     *            resolved by type
     * @return the tool with the given type, if unique, or the tool with the
     *         given name and type
     * @exception org.toolboxframework.NoUniqueToolForTypeException
     *                if the toolbox contains more than one tool of the given
     *                type and none of the names of those tools equal the
     *                provided fallback name
     * @exception org.toolboxframework.ToolNotFoundException
     *                if the toolbox does not contain a tool that matches the
     *                given object type
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T getTool(final Class<T> requiredType, final String fallbackName) {
        Validate.argNotNull(requiredType, "requiredType");
        Validate.argNotBlank(fallbackName, "fallbackName");

        final List<String> matchingToolNames = TOOL_NAMES_BY_CLASS.get(requiredType);

        if (null == matchingToolNames || matchingToolNames.isEmpty()) {
            throw new ToolNotFoundException(String.format("required type: %s", requiredType.getName()));
        }

        if (matchingToolNames.size() > 1) {
            // check that the fallback name will work before using it
            if (requiredType.equals(getType(fallbackName))) {
                TOOLS_BY_NAME.get(fallbackName);
            }

            throw new NoUniqueToolForTypeException(
                    String.format("required type: %s, fallback name (%s) did not match any of the options",
                            requiredType.getName(), fallbackName));
        }

        return (T) TOOLS_BY_NAME.get(matchingToolNames.get(0));
    }

    /**
     * Return the tool that uniquely matches the given object type.
     *
     * @param requiredType
     *            type the tool must match (can be an interface or superclass of
     *            the actual class)
     * @return the tool that uniquely matches the given object type
     * @exception org.toolboxframework.NoUniqueToolForTypeException
     *                if more than one tool of the given type was found
     * @exception org.toolboxframework.ToolNotFoundException
     *                if the toolbox does not contain a tool that matches the
     *                given object type
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T getTool(final Class<T> requiredType) {
        Validate.argNotNull(requiredType, "requiredType");

        final List<String> matchingToolNames = TOOL_NAMES_BY_CLASS.get(requiredType);

        if (null == matchingToolNames || matchingToolNames.isEmpty()) {
            throw new ToolNotFoundException(String.format("required type: %s", requiredType.getName()));
        }

        if (matchingToolNames.size() > 1) {
            throw new NoUniqueToolForTypeException(String.format("required type: %s", requiredType.getName()));
        }

        return (T) TOOLS_BY_NAME.get(matchingToolNames.get(0));
    }

    /**
     * Return the tool with the given name and type.
     * <p>
     * Behaves the same as getTool({@link java.lang.String}), but provides a
     * measure of type safety by throwing a ToolNotOfRequiredTypeException if
     * the tool is not of the required type. This means that ClassCastException
     * can't be thrown on casting the result correctly, as can happen with
     * getTool({@link java.lang.String}).
     *
     * @param toolName
     *            the name of the tool to query
     * @param requiredType
     *            type the tool must match (can be an interface or superclass of
     *            the actual class)
     * @return the tool with the given name and type
     * @exception org.toolboxframework.ToolNotFoundException
     *                if the toolbox does not contain a tool with the given name
     * @exception org.toolboxframework.ToolNotOfRequiredTypeException
     *                if the tool is not of the required type
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> T getTool(final String toolName, final Class<T> requiredType) {
        Validate.argNotBlank(toolName, "toolName");
        Validate.argNotNull(requiredType, "requiredType");

        if (!containsTool(toolName)) {
            throw new ToolNotFoundException(String.format("tool name: %s", toolName));
        }

        final Object tool = TOOLS_BY_NAME.get(toolName);

        final Class<?> resolvedType = PRIMITIVE_TYPES_TO_OBJECT_TYPES.keySet().contains(requiredType)
                ? PRIMITIVE_TYPES_TO_OBJECT_TYPES.get(requiredType) : requiredType;

        if (!resolvedType.isAssignableFrom(tool.getClass())) {
            throw new ToolNotOfRequiredTypeException(String.format("tool name: %s, required type: %s, actual type: %s",
                    toolName, requiredType.getName(), tool.getClass().getName()));
        }

        return (T) tool;
    }

    /**
     * Return the tool with the given name.
     *
     * @param toolName
     *            the name of the tool to query
     * @return the tool with the given name
     * @exception org.toolboxframework.ToolNotFoundException
     *                if the toolbox does not contain a tool with the given name
     */
    public static synchronized Object getTool(final String toolName) {
        Validate.argNotBlank(toolName, "toolName");

        if (!containsTool(toolName)) {
            throw new ToolNotFoundException(String.format("tool name: %s", toolName));
        }

        return TOOLS_BY_NAME.get(toolName);
    }

    /**
     * Determine the type of the tool with the given name. More specifically,
     * determine the type of object that getTool({@link java.lang.String}) would
     * return for the given name.
     *
     * @param toolName
     *            the name of the tool to query
     * @return the type of the tool, or null if the toolbox does not contain a
     *         tool with the given name
     */
    public static synchronized Class<?> getType(final String toolName) {
        Validate.argNotBlank(toolName, "toolName");

        if (!containsTool(toolName)) {
            return null;
        }

        return getTool(toolName).getClass();
    }

    /**
     * Does the toolbox contain a tool with the given name?
     *
     * @param toolName
     *            the name of the tool to query
     * @return whether the toolbox contains a tool with the given name
     */
    public static synchronized boolean containsTool(final String toolName) {
        Validate.argNotBlank(toolName, "toolName");

        return TOOLS_BY_NAME.containsKey(toolName);
    }

    /**
     * Clears all tools from the toolbox.
     * <p>
     * The toolbox will contain no tools after this call completes. Use wisely.
     *
     * @see org.toolboxframework.scan.ToolScanner#clearScanHistory()
     */
    public static synchronized void clear() {
        TOOL_NAMES_BY_CLASS.clear();
        TOOLS_BY_NAME.clear();
    }

    /**
     * Return all the tools currently in the toolbox of the given type.
     *
     * @param clazz
     *            the type of the tools to return
     * @return the tools with the given type
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> Map<String, T> getTools(final Class<T> clazz) {
        Validate.argNotNull(clazz, "clazz");

        final Map<String, T> tools = new HashMap<String, T>();
        for (final String toolName : TOOL_NAMES_BY_CLASS.get(clazz)) {
            tools.put(toolName, (T) TOOLS_BY_NAME.get(toolName));
        }

        return Collections.unmodifiableMap(tools);
    }

    /**
     * Return the names of all the tools currently in the toolbox of the given
     * type.
     *
     * @param clazz
     *            the type with which to query for tool names
     * @return the names of the tools of the given type
     */
    public static synchronized List<String> getToolNames(final Class<?> clazz) {
        Validate.argNotNull(clazz, "clazz");

        final List<String> toolNames = TOOL_NAMES_BY_CLASS.get(clazz);
        if (null == toolNames) {
            return null;
        }

        return Collections.unmodifiableList(toolNames);
    }
}
