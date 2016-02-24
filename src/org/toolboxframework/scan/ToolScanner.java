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

package org.toolboxframework.scan;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.toolboxframework.NoUniqueToolForTypeException;
import org.toolboxframework.ToolNameAlreadyInUseException;
import org.toolboxframework.ToolNotFoundException;
import org.toolboxframework.Toolbox;
import org.toolboxframework.inject.ToolInjector;
import org.toolboxframework.inject.annotation.UseTool;
import org.toolboxframework.scan.annotation.Tool;
import org.toolboxframework.scan.annotation.ToolMaker;
import org.toolboxframework.validate.Validate;

/**
 * The ToolScanner class contains methods to scan classes and packages for
 * tools.
 *
 * @author T.C.C.
 * @version 1.0
 * @see org.toolboxframework.scan.annotation.Tool
 * @see org.toolboxframework.scan.annotation.ToolMaker
 */
public class ToolScanner {
    private static final Logger LOGGER = Logger.getLogger(ToolScanner.class.getName());
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");
    private static final String CLASS_PATH = System.getProperty("java.class.path");
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String TOOL_ANNOTATION_UTF8 = "Lorg/toolboxframework/scan/annotation/Tool;";
    private static final String USE_TOOL_ANNOTATION_UTF8 = "Lorg/toolboxframework/inject/annotation/UseTool;";
    private static final Set<Class<?>> ALREADY_SCANNED_CLASSES = new HashSet<Class<?>>();
    private final static Map<Class<?>, Class<?>> PRIMITIVE_TYPE_WRAPPERS = new HashMap<Class<?>, Class<?>>();

    static {
        PRIMITIVE_TYPE_WRAPPERS.put(Boolean.class, boolean.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Byte.class, byte.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Character.class, char.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Double.class, double.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Float.class, float.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Integer.class, int.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Long.class, long.class);
        PRIMITIVE_TYPE_WRAPPERS.put(Short.class, short.class);

        PRIMITIVE_TYPE_WRAPPERS.put(boolean.class, Boolean.class);
        PRIMITIVE_TYPE_WRAPPERS.put(byte.class, Byte.class);
        PRIMITIVE_TYPE_WRAPPERS.put(char.class, Character.class);
        PRIMITIVE_TYPE_WRAPPERS.put(double.class, Double.class);
        PRIMITIVE_TYPE_WRAPPERS.put(float.class, Float.class);
        PRIMITIVE_TYPE_WRAPPERS.put(int.class, Integer.class);
        PRIMITIVE_TYPE_WRAPPERS.put(long.class, Long.class);
        PRIMITIVE_TYPE_WRAPPERS.put(short.class, Short.class);
    }

    private ToolScanner() {
    }

    /**
     * Scans the given base package for tools, adding them to the toolbox.
     *
     * @param basePackage
     *            the base package to scan for tools
     */
    public static void scanPackage(final String basePackage) {
        Validate.argNotBlank(basePackage, "basePackage");

        synchronized (Toolbox.class) {
            scanPackages(new String[] { basePackage });
        }
    }

    /**
     * Scans the given base packages for tools, adding them to the toolbox.
     *
     * @param basePackages
     *            the base packages to scan for tools
     */
    public static void scanPackages(final Collection<? extends String> basePackages) {
        Validate.argNotEmpty(basePackages, "basePackages");

        synchronized (Toolbox.class) {
            scanPackages(basePackages.toArray(new String[basePackages.size()]));
        }
    }

    /**
     * Scans the given base packages for tools, adding them to the toolbox.
     *
     * @param basePackages
     *            the base packages to scan for tools
     */
    public static void scanPackages(final String[] basePackages) {
        Validate.argNotEmpty(basePackages, "basePackages");

        synchronized (Toolbox.class) {
            final String[] normalizedBasePackages = validateAndNormalizeBasePackages(basePackages);

            final List<Class<?>> classesToCheckForAnnotations = new ArrayList<Class<?>>();

            final Set<String> pathsToClasses = new HashSet<String>(Arrays.asList(CLASS_PATH.split(PATH_SEPARATOR)));
            for (final String pathToClasses : pathsToClasses) {
                final File file = new File(pathToClasses);
                if (file.isDirectory()) {
                    classesToCheckForAnnotations.addAll(getClassesToCheckForAnnotations(file, normalizedBasePackages));
                } else if (isZipFile(file)) {
                    JarFile jarFile = null;
                    try {
                        jarFile = new JarFile(pathToClasses);
                    } catch (final IOException e) {
                        throw new ToolScannerIOException(String.format("jar file: %s", pathToClasses), e);
                    }
                    classesToCheckForAnnotations
                            .addAll(getClassesToCheckForAnnotations(jarFile, normalizedBasePackages));
                }
            }

            scanClasses(classesToCheckForAnnotations);
        }
    }

    /**
     * Scans the given class for tools, adding them to the toolbox.
     *
     * @param clazz
     *            the class to scan for tools
     */
    public static void scanClass(final Class<?> clazz) {
        Validate.argNotNull(clazz, "clazz");

        synchronized (Toolbox.class) {
            scanClasses(new Class<?>[] { clazz });
        }
    }

    /**
     * Scans the given classes for tools, adding them to the toolbox.
     *
     * @param classes
     *            the classes to scan for tools
     */
    public static void scanClasses(final Collection<? extends Class<?>> classes) {
        Validate.argNotEmpty(classes, "classes");

        synchronized (Toolbox.class) {
            scanClasses(classes.toArray(new Class<?>[classes.size()]));
        }
    }

    /**
     * Scans the given classes for tools, adding them to the toolbox.
     *
     * @param classes
     *            the classes to scan for tools
     */
    public static void scanClasses(final Class<?>[] classes) {
        Validate.argNotEmpty(classes, "classes");

        synchronized (Toolbox.class) {
            final Map<String, ToolDescription> toolDescriptions = new HashMap<String, ToolDescription>();
            final List<Class<?>> nonToolClassesOnWhichToPerformStaticFieldInjection = new ArrayList<Class<?>>();

            for (final Class<?> classToCheckForAnnotation : classes) {
                if (classToCheckForAnnotation.isAnnotationPresent(Tool.class)) {
                    if (ALREADY_SCANNED_CLASSES.contains(classToCheckForAnnotation)) {
                        continue;
                    }

                    if (!isConcreteNonEnumClass(classToCheckForAnnotation)) {
                        throw new AnnotatedClassNotConcreteNonEnumTypeException(
                                String.format("class: %s", classToCheckForAnnotation.getName()));
                    }

                    try {
                        classToCheckForAnnotation.getDeclaredConstructor();
                    } catch (final NoSuchMethodException e) {
                        throw new MissingDefaultConstructorException(
                                String.format("class: %s", classToCheckForAnnotation.getName()), e);
                    }

                    final ToolDescription toolInstanceDescription = createInstanceLevelToolDescriptionForClass(
                            classToCheckForAnnotation);
                    if (toolDescriptions.containsKey(toolInstanceDescription.name)) {
                        throw new DuplicateToolNameException(
                                String.format("tool name: %s", toolInstanceDescription.name));
                    } else {
                        toolDescriptions.put(toolInstanceDescription.name, toolInstanceDescription);
                    }

                    final ToolDescription toolClassDescription = createClassLevelToolDescriptionForClass(
                            classToCheckForAnnotation);
                    if (toolDescriptions.containsKey(toolClassDescription.name)) {
                        throw new DuplicateToolNameException(String.format("tool name: %s", toolClassDescription.name));
                    } else {
                        toolDescriptions.put(toolClassDescription.name, toolClassDescription);
                    }
                    ALREADY_SCANNED_CLASSES.add(classToCheckForAnnotation);
                } else if (classToCheckForAnnotation.isAnnotationPresent(ToolMaker.class)) {
                    if (ALREADY_SCANNED_CLASSES.contains(classToCheckForAnnotation)) {
                        continue;
                    }

                    if (!isConcreteNonEnumClass(classToCheckForAnnotation)) {
                        throw new AnnotatedClassNotConcreteNonEnumTypeException(
                                String.format("class: %s", classToCheckForAnnotation.getName()));
                    }

                    final Set<ToolDescription> toolDescriptionsFromToolMaker = createToolDescriptionsFromClassAnnotatedWithToolMaker(
                            classToCheckForAnnotation);
                    for (final ToolDescription toolDescription : toolDescriptionsFromToolMaker) {
                        if (toolDescriptions.containsKey(toolDescription.name)) {
                            throw new DuplicateToolNameException(String.format("tool name: %s", toolDescription.name));
                        } else {
                            toolDescriptions.put(toolDescription.name, toolDescription);
                        }
                    }
                    ALREADY_SCANNED_CLASSES.add(classToCheckForAnnotation);
                } else {
                    // check for static fields annotated with @UseTool
                    for (final Field field : classToCheckForAnnotation.getDeclaredFields()) {
                        if (field.isAnnotationPresent(UseTool.class) && Modifier.isStatic(field.getModifiers())) {
                            nonToolClassesOnWhichToPerformStaticFieldInjection.add(classToCheckForAnnotation);
                        }
                    }
                }
            }

            buildTools(toolDescriptions);

            // finish it up with static field injection
            for (final Class<?> classOnWhichToPerformStaticFieldInjection : nonToolClassesOnWhichToPerformStaticFieldInjection) {
                ToolInjector.injectTools(classOnWhichToPerformStaticFieldInjection);
            }
        }
    }

    /**
     * Clears all scanning history. After a call to this method, the tool
     * scanner will not ignore previously scanned classes on a subsequent call
     * to one of the scan methods, possibly resulting in a
     * {@link org.toolboxframework.ToolNameAlreadyInUseException} being thrown.
     *
     * @see org.toolboxframework.Toolbox#clear()
     */
    public static void clearScanHistory() {
        synchronized (Toolbox.class) {
            ALREADY_SCANNED_CLASSES.clear();
        }
    }

    private static boolean isConcreteNonEnumClass(final Class<?> clazz) {
        return !(clazz.isEnum() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()));
    }

    private static boolean isZipFile(final File file) {
        boolean isZipFile = false;

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            isZipFile = 0x504B0304 == randomAccessFile.readInt();
        } catch (final IOException e) {
            throw new ToolScannerIOException(String.format("file: %s", file.getAbsolutePath()), e);
        } finally {
            close(randomAccessFile);
        }

        return isZipFile;
    }

    private static String[] validateAndNormalizeBasePackages(final String[] basePackages) {
        Arrays.sort(basePackages);

        final int length = basePackages.length;
        int count = length;
        int i = 0;
        while (i < length) {
            final String prefix = basePackages[i];
            Validate.isJavaPackageName(prefix, prefix);

            int j = i + 1;
            while (j < length) {
                final String nextPackage = basePackages[j];
                Validate.isJavaPackageName(nextPackage, nextPackage);
                if (nextPackage.startsWith(prefix)) {
                    basePackages[j] = null;
                    count--;
                    j++;
                } else {
                    break;
                }
            }

            i = j;
        }

        final String[] normalizedBasePackages = new String[count];
        int a = 0;
        for (final String basePackage : basePackages) {
            if (basePackage != null) {
                normalizedBasePackages[a] = basePackage;
                ++a;
            }
        }

        return normalizedBasePackages;
    }

    private static String replaceAllPeriodsWithFileSeparator(final String value) {
        if (FILE_SEPARATOR.equals("\\")) {
            return value.replaceAll("\\.", "\\\\");
        }

        return value.replaceAll("\\.", FILE_SEPARATOR);
    }

    private static List<Class<?>> getClassesToCheckForAnnotations(final File baseDirectory,
            final String[] basePackages) {
        final List<Class<?>> classesToCheckForAnnotations = new ArrayList<Class<?>>();
        final URI baseDirectoryUri = baseDirectory.toURI();

        final Stack<File> subdirectories = new Stack<File>();
        for (final String basePackage : basePackages) {
            final File basePackageDirectory = new File(baseDirectory, replaceAllPeriodsWithFileSeparator(basePackage));
            if (basePackageDirectory.isDirectory()) {
                subdirectories.push(basePackageDirectory);
            }
        }

        while (!subdirectories.empty()) {
            final File currentDirectory = subdirectories.pop();
            final File[] files = currentDirectory.listFiles();
            for (final File currentFile : files) {
                if (currentFile.isDirectory()) {
                    subdirectories.push(currentFile);
                } else {
                    if (!currentFile.getName().endsWith(".class")) {
                        continue;
                    }

                    final String relativePathToClassFile = baseDirectoryUri.relativize(currentFile.toURI()).getPath();
                    final String className = relativePathToClassFile.replaceAll("/", "\\.").replaceAll(".class$", "");
                    if (isClassAlreadyLoaded(className)) {
                        try {
                            final Class<?> clazz = Class.forName(className);
                            classesToCheckForAnnotations.add(clazz);
                        } catch (final ClassNotFoundException e) {
                            throw new ToolScannerIOException(String.format("class: %s", className), e);
                        }
                    } else {
                        // determine if the annotation is present using byte
                        // code analysis
                        InputStream inputStream = null;
                        try {
                            inputStream = new FileInputStream(currentFile);
                        } catch (final IOException e) {
                            throw new ToolScannerIOException(String.format("class: %s", className), e);
                        }
                        final boolean hasAnnotation = hasAnnotation(inputStream);

                        if (hasAnnotation) {
                            final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                            try {
                                final Class<?> clazz = classLoader.loadClass(className);
                                classesToCheckForAnnotations.add(clazz);
                            } catch (final ClassNotFoundException e) {
                                throw new ToolScannerIOException(String.format("class: %s", className), e);
                            }
                        }
                    }
                }
            }
        }

        return classesToCheckForAnnotations;
    }

    private static List<Class<?>> getClassesToCheckForAnnotations(final JarFile jarFile, final String[] basePackages) {
        final List<JarEntry> classFileJarEntriesUnderBasePackages = getClassFileJarEntriesUnderBasePackages(jarFile,
                basePackages);

        final List<Class<?>> classesToCheckForAnnotations = new ArrayList<Class<?>>();

        for (final JarEntry jarEntry : classFileJarEntriesUnderBasePackages) {
            final String className = jarEntry.getName().replaceAll("/", "\\.").replaceAll(".class$", "");
            if (isClassAlreadyLoaded(className)) {
                try {
                    final Class<?> clazz = Class.forName(className);
                    classesToCheckForAnnotations.add(clazz);
                } catch (final ClassNotFoundException e) {
                    throw new ToolScannerIOException(String.format("class: %s", className), e);
                }
            } else {
                // determine if one of the annotations is present via byte code
                // analysis
                InputStream inputStream = null;
                try {
                    inputStream = jarFile.getInputStream(jarEntry);
                } catch (final IOException e) {
                    throw new ToolScannerIOException(String.format("class: %s", className), e);
                }
                final boolean hasAnnotation = hasAnnotation(inputStream);

                if (hasAnnotation) {
                    final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                    try {
                        final Class<?> clazz = classLoader.loadClass(className);
                        classesToCheckForAnnotations.add(clazz);
                    } catch (final ClassNotFoundException e) {
                        throw new ToolScannerIOException(String.format("class: %s", className), e);
                    }
                }
            }
        }

        return classesToCheckForAnnotations;
    }

    private static Set<ToolDescription> createToolDescriptionsFromClassAnnotatedWithToolMaker(
            final Class<?> classAnnotatedWithToolMaker) {
        final Set<ToolDescription> toolDescriptions = new HashSet<ToolDescription>();
        // Add the tool maker instance and class tools to the list of tool
        // descriptions that are returned
        final ToolDescription toolMakerInstanceToolDescription = createInstanceLevelToolDescriptionForClass(
                classAnnotatedWithToolMaker);
        toolDescriptions.add(toolMakerInstanceToolDescription);
        final ToolDescription toolMakerClassToolDescription = createClassLevelToolDescriptionForClass(
                classAnnotatedWithToolMaker);
        toolDescriptions.add(toolMakerClassToolDescription);

        // All tools made in the tool maker depend on the tool maker itself
        final DependencyDescription toolMakerInstanceDependencyDescription = new DependencyDescription(
                toolMakerInstanceToolDescription.type,
                toolMakerInstanceToolDescription.name, null);
        final DependencyDescription toolMakerClassDependencyDescription = new DependencyDescription(
                toolMakerClassToolDescription.type,
                toolMakerClassToolDescription.name, null);

        final Method[] methods = classAnnotatedWithToolMaker.getDeclaredMethods();
        for (final Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {

                if (method.getReturnType().equals(Void.TYPE)) {
                    throw new MethodAnnotatedWithToolReturnsVoidException(
                            String.format("method: %s", method.toString()));
                }

                // include the parameters as dependencies
                final Class<?>[] parameterTypes = method.getParameterTypes();
                final Annotation[][] parameterAnnotations = method.getParameterAnnotations();

                final DependencyDescription[] parameterDependencies = new DependencyDescription[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; ++i) {
                    String explicitName = null;
                    for (int j = 0; j < parameterAnnotations[i].length; ++j) {
                        if (parameterAnnotations[i][j].annotationType().equals(UseTool.class)) {
                            final String nameFromAnnotation = ((UseTool) parameterAnnotations[i][j]).value();
                            if (nameFromAnnotation != null && !nameFromAnnotation.equals("")) {
                                explicitName = nameFromAnnotation;
                            }
                        }
                    }
                    parameterDependencies[i] = new DependencyDescription(parameterTypes[i], explicitName, null);
                }

                // use whatever string is provided in the method's Tool
                // annotation
                // or, if nothing is provided there, the name of the method,
                // as the tool name.
                String toolName = method.getName();
                final String nameFromAnnotation = method.getAnnotation(Tool.class).value();
                if (!nameFromAnnotation.equals("")) {
                    toolName = nameFromAnnotation;
                }

                final List<DependencyDescription> actualDependencies = new ArrayList<DependencyDescription>(
                        Arrays.asList(parameterDependencies));
                if (Modifier.isStatic(method.getModifiers())) {
                    // add the tool maker class (and indirectly all static
                    // fields) as a dependency
                    actualDependencies.add(toolMakerClassDependencyDescription);
                } else {
                    // add the tool maker instance (and indirectly all instance
                    // fields, the class, and all static fields) as a dependency
                    actualDependencies.add(toolMakerInstanceDependencyDescription);
                }

                // use the type returned by the method as the tool type
                final ToolDescription toolDescription = new ToolDescription(method.getReturnType(),
                        toolName,
                        actualDependencies,
                        new ToolMakerMethodToolBuilder(toolMakerInstanceToolDescription, method,
                                parameterDependencies));

                toolDescriptions.add(toolDescription);
            }
        }
        return toolDescriptions;
    }

    private static void buildTools(final Map<String, ToolDescription> descriptionsOfToolsToBuild) {
        final Map<Class<?>, List<ToolDescription>> classesMatchingDescriptionsOfToolsNotYetBuilt = new HashMap<Class<?>, List<ToolDescription>>();
        final Stack<ToolDescription> descriptionsOfToolsToBuildStack = new Stack<ToolDescription>();

        for (final ToolDescription descriptionOfTool : descriptionsOfToolsToBuild.values()) {
            // check if any of the tools that are to be built already exist in
            // the
            // toolbox before continuing
            if (Toolbox.containsTool(descriptionOfTool.name)) {
                throw new ToolNameAlreadyInUseException(String.format("tool name: %s", descriptionOfTool.name));
            }

            descriptionsOfToolsToBuildStack.push(descriptionOfTool);
            associateToolDescriptionWithAllApplicableClasses(classesMatchingDescriptionsOfToolsNotYetBuilt,
                    descriptionOfTool);
        }

        final Map<String, ToolDescription> descriptionsOfToolsBeingBuiltNowMap = new HashMap<String, ToolDescription>();
        while (!descriptionsOfToolsToBuildStack.isEmpty()) {
            final ToolDescription descriptionOfToolCurrentlyBeingBuilt = descriptionsOfToolsToBuildStack.peek();

            // has it already been built? (this time around it's OK)
            if (Toolbox.containsTool(descriptionOfToolCurrentlyBeingBuilt.name)) {
                descriptionsOfToolsToBuildStack.pop();
                continue;
            }

            descriptionsOfToolsBeingBuiltNowMap.put(descriptionOfToolCurrentlyBeingBuilt.name,
                    descriptionOfToolCurrentlyBeingBuilt);

            LOGGER.log(Level.FINE, "Checking if we can build tool with name={0} and type={1}",
                    new Object[] { descriptionOfToolCurrentlyBeingBuilt.name,
                            descriptionOfToolCurrentlyBeingBuilt.type.getName() });

            boolean foundDependencyThatHasntBeenBuiltYet = false;
            for (final DependencyDescription dependencyDescription : descriptionOfToolCurrentlyBeingBuilt.dependencies) {
                // if we have found a dependency that hasn't been built,
                // don't bother iterating other additional dependencies
                if (foundDependencyThatHasntBeenBuiltYet) {
                    break;
                }

                if (dependencyDescription.explicitName != null) {
                    LOGGER.log(Level.FINER, "Checking dependency with explicit name={0} and type={1}",
                            new Object[] { dependencyDescription.explicitName,
                                    dependencyDescription.type.getName() });

                    // does the dependency already exist in the toolbox?
                    if (Toolbox.containsTool(dependencyDescription.explicitName)) {
                        continue;
                    }

                    foundDependencyThatHasntBeenBuiltYet = true;

                    // determine if the description of the dependency is
                    // enough to determine that we have a circular
                    // dependency
                    if (descriptionsOfToolsBeingBuiltNowMap.containsKey(dependencyDescription.explicitName)) {
                        throw new CircularDependencyException(getCircularDependencyExceptionMessage(
                                descriptionsOfToolsToBuildStack, dependencyDescription.explicitName));
                    } else if (descriptionsOfToolsToBuild.containsKey(dependencyDescription.explicitName)) {
                        final ToolDescription toolDescriptionMatchingDependency = descriptionsOfToolsToBuild
                                .get(dependencyDescription.explicitName);
                        descriptionsOfToolsToBuildStack.push(toolDescriptionMatchingDependency);
                        descriptionsOfToolsBeingBuiltNowMap.put(toolDescriptionMatchingDependency.name,
                                toolDescriptionMatchingDependency);
                    } else {
                        throw new ToolNotFoundException(
                                String.format("tool name: %s", dependencyDescription.explicitName));
                    }
                } else {
                    LOGGER.log(Level.FINER, "Checking dependency with type={0} and fallback name={1}",
                            new Object[] { dependencyDescription.type.getName(),
                                    dependencyDescription.fallbackName });

                    // get all the candidates (including tools already in
                    // the toolbox)
                    // if the number of candidates is greater than one
                    // insist on using the fall back name
                    // else if there is no fall back name, throw exception
                    // else if there is a fall back name, insist on finding a
                    // tool of that name

                    final List<String> namesOfToolsAlreadyInToolboxMatchingDependencyType = Toolbox
                            .getToolNames(dependencyDescription.type);
                    final List<ToolDescription> descriptionsOfToolsNotYetBuiltThatMatchDependencyType = classesMatchingDescriptionsOfToolsNotYetBuilt
                            .get(dependencyDescription.type);

                    final int numberOfCandidates = ((namesOfToolsAlreadyInToolboxMatchingDependencyType == null) ? 0
                            : namesOfToolsAlreadyInToolboxMatchingDependencyType.size()) +
                            ((descriptionsOfToolsNotYetBuiltThatMatchDependencyType == null) ? 0
                                    : descriptionsOfToolsNotYetBuiltThatMatchDependencyType.size());

                    if (numberOfCandidates == 0) {
                        throw new ToolNotFoundException(
                                String.format("required type: %s", dependencyDescription.type.getName()));
                    } else if (numberOfCandidates == 1) {
                        // if the dependency is already in the toolbox, we're
                        // good, continue to check other dependencies
                        if (namesOfToolsAlreadyInToolboxMatchingDependencyType != null
                                && !namesOfToolsAlreadyInToolboxMatchingDependencyType.isEmpty()) {
                            continue;
                        }

                        final ToolDescription dependencyToolDescription = descriptionsOfToolsNotYetBuiltThatMatchDependencyType
                                .get(0);

                        if (descriptionsOfToolsBeingBuiltNowMap.containsKey(dependencyToolDescription.name)) {
                            throw new CircularDependencyException(getCircularDependencyExceptionMessage(
                                    descriptionsOfToolsToBuildStack, dependencyToolDescription.name));
                        }

                        // else add the tool to the stack and try to build
                        // it next
                        foundDependencyThatHasntBeenBuiltYet = true;
                        descriptionsOfToolsToBuildStack.push(dependencyToolDescription);
                        descriptionsOfToolsBeingBuiltNowMap.put(dependencyToolDescription.name,
                                dependencyToolDescription);
                    }
                    // more than one candidate
                    else {
                        // we need to use the fall back name
                        if (dependencyDescription.fallbackName == null) {
                            throw new NoUniqueToolForTypeException(
                                    String.format("required type: %s", dependencyDescription.type.getName()));
                        }

                        // if the toolbox contains it, we're good
                        else if (namesOfToolsAlreadyInToolboxMatchingDependencyType
                                .contains((dependencyDescription.fallbackName))) {
                            continue;
                        }

                        boolean foundToolToBeBuiltWithMatchingName = false;
                        if (descriptionsOfToolsNotYetBuiltThatMatchDependencyType != null) {
                            // check if any of the tools left to be built
                            // have matching name
                            for (final ToolDescription possibleToolToUseForDependency : descriptionsOfToolsNotYetBuiltThatMatchDependencyType) {
                                if (possibleToolToUseForDependency.name
                                        .equals(dependencyDescription.fallbackName)) {

                                    if (descriptionsOfToolsBeingBuiltNowMap
                                            .containsKey(possibleToolToUseForDependency.name)) {
                                        throw new CircularDependencyException(getCircularDependencyExceptionMessage(
                                                descriptionsOfToolsToBuildStack, possibleToolToUseForDependency.name));
                                    }

                                    // found a tool waiting to be built with
                                    // matching name
                                    // add it to top of the stack and built
                                    // it next
                                    foundDependencyThatHasntBeenBuiltYet = true;
                                    descriptionsOfToolsToBuildStack.push(possibleToolToUseForDependency);
                                    descriptionsOfToolsBeingBuiltNowMap.put(possibleToolToUseForDependency.name,
                                            possibleToolToUseForDependency);

                                    foundToolToBeBuiltWithMatchingName = true;
                                    break;
                                }
                            }
                        }

                        if (!foundToolToBeBuiltWithMatchingName) {
                            throw new NoUniqueToolForTypeException(String.format("required type: %s%s",
                                    dependencyDescription.type.getName(),
                                    (dependencyDescription.fallbackName == null) ? ""
                                            : String.format(", fallback name (%s) did not match any of the options",
                                                    dependencyDescription.fallbackName)));
                        }
                    }
                }
            }

            if (!foundDependencyThatHasntBeenBuiltYet) {
                final Object tool = descriptionOfToolCurrentlyBeingBuilt.builder.buildTool();

                LOGGER.log(Level.FINE, "Built tool with name={0} and type={1}. Adding to toolbox.",
                        new Object[] { descriptionOfToolCurrentlyBeingBuilt.name,
                                descriptionOfToolCurrentlyBeingBuilt.type.getName() });

                Toolbox.addTool(tool, descriptionOfToolCurrentlyBeingBuilt.name);
                descriptionsOfToolsToBuildStack.pop();
                descriptionsOfToolsBeingBuiltNowMap.remove(descriptionOfToolCurrentlyBeingBuilt.name);
                disassociateToolMetadataWithAllApplicableClasses(classesMatchingDescriptionsOfToolsNotYetBuilt,
                        descriptionOfToolCurrentlyBeingBuilt);
            }
        }
    }

    private static String getCircularDependencyExceptionMessage(final Stack<ToolDescription> toolsToBuildStack,
            final String name) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        while (!toolsToBuildStack.empty()) {
            final ToolDescription toolDescription = toolsToBuildStack.pop();
            stringBuilder.insert(0, String.format("%s -> ", toolDescription.name));
            if (name.equals(toolDescription.name)) {
                break;
            }
        }
        return stringBuilder.toString();
    }

    private static void disassociateToolMetadataWithAllApplicableClasses(
            final Map<Class<?>, List<ToolDescription>> toolTypesToListOfToolMetadataNotYetBuilt,
            final ToolDescription toolDescription) {
        final Set<Class<?>> assignableClasses = getAllAssignableClasses(toolDescription.type);
        for (final Class<?> clazz : assignableClasses) {
            if (toolTypesToListOfToolMetadataNotYetBuilt.containsKey(clazz)
                    && toolTypesToListOfToolMetadataNotYetBuilt.get(clazz) != null) {
                toolTypesToListOfToolMetadataNotYetBuilt.get(clazz).remove(toolDescription);
            }
        }
    }

    private static ToolDescription createInstanceLevelToolDescriptionForClass(final Class<?> toolType) {
        // default to the uncapitalized class name
        String toolName = uncapitalize(toolType.getSimpleName());
        String toolNameFromAnnotation = null;
        if (toolType.isAnnotationPresent(ToolMaker.class)) {
            toolNameFromAnnotation = toolType.getAnnotation(ToolMaker.class).value();
        } else if (toolType.isAnnotationPresent(Tool.class)) {
            toolNameFromAnnotation = toolType.getAnnotation(Tool.class).value();
        }

        if (!toolNameFromAnnotation.equals("")) {
            toolName = toolNameFromAnnotation;
        }

        // the instance tool depends on all instance fields
        final List<DependencyDescription> dependencies = new ArrayList<DependencyDescription>();
        for (final Field field : toolType.getDeclaredFields()) {
            if (field.isAnnotationPresent(UseTool.class) && !Modifier.isStatic(field.getModifiers())) {
                final Class<?> dependencyType = field.getType();
                final String nameFromAnnotation = field.getAnnotation(UseTool.class).value();
                String explicitName = null;
                if (nameFromAnnotation != null && !nameFromAnnotation.equals("")) {
                    explicitName = nameFromAnnotation;
                }

                dependencies.add(new DependencyDescription(dependencyType, explicitName, field.getName()));
            }
        }

        // Add the class as a dependency
        final DependencyDescription toolMakerClassDependencyDescription = new DependencyDescription(Class.class,
                toolType.getName(), null);
        dependencies.add(toolMakerClassDependencyDescription);

        return new ToolDescription(toolType, toolName, dependencies, new DefaultConstructorToolBuilder(toolType));
    }

    private static ToolDescription createClassLevelToolDescriptionForClass(final Class<?> toolType) {
        // use the fully qualified class name
        final String toolName = toolType.getName();

        // the class tool depends on the class's static fields
        final List<DependencyDescription> dependencies = new ArrayList<DependencyDescription>();
        for (final Field field : toolType.getDeclaredFields()) {
            if (field.isAnnotationPresent(UseTool.class) && Modifier.isStatic(field.getModifiers())) {
                final Class<?> dependencyType = field.getType();
                final String nameFromAnnotation = field.getAnnotation(UseTool.class).value();
                String explicitName = null;
                if (nameFromAnnotation != null && !nameFromAnnotation.equals("")) {
                    explicitName = nameFromAnnotation;
                }

                dependencies.add(new DependencyDescription(dependencyType, explicitName, field.getName()));
            }
        }

        return new ToolDescription(Class.class, toolName, dependencies, new ClassToolBuilder(toolType));
    }

    private static String uncapitalize(final String value) {
        final char c[] = value.toCharArray();
        if (c[0] >= 'A' && c[0] <= 'Z') {
            c[0] += 32;
        }
        return new String(c);
    }

    private static void associateToolDescriptionWithAllApplicableClasses(
            final Map<Class<?>, List<ToolDescription>> toolTypesToListOfToolMetadataNotYetBuilt,
            final ToolDescription toolDescription) {
        final Set<Class<?>> assignableClasses = getAllAssignableClasses(toolDescription.type);
        for (final Class<?> clazz : assignableClasses) {
            if (toolTypesToListOfToolMetadataNotYetBuilt.containsKey(clazz)
                    && toolTypesToListOfToolMetadataNotYetBuilt.get(clazz) != null) {
                toolTypesToListOfToolMetadataNotYetBuilt.get(clazz).add(toolDescription);
            } else {
                final List<ToolDescription> toolMetadataList = new ArrayList<ToolDescription>();
                toolMetadataList.add(toolDescription);
                toolTypesToListOfToolMetadataNotYetBuilt.put(clazz, toolMetadataList);
            }
        }
    }

    private static Set<Class<?>> getAllAssignableClasses(final Class<?> objectClass) {
        final Set<Class<?>> assignableClasses = new HashSet<Class<?>>();
        final Queue<Class<?>> queue = new LinkedList<Class<?>>();
        queue.add(objectClass);

        while (!queue.isEmpty()) {
            final Class<?> currentClass = queue.poll();
            if (currentClass == null) {
                continue;
            }
            assignableClasses.add(currentClass);
            if (PRIMITIVE_TYPE_WRAPPERS.containsKey(currentClass)) {
                assignableClasses.add(PRIMITIVE_TYPE_WRAPPERS.get(currentClass));
            }
            Collections.addAll(queue, currentClass.getInterfaces());
            queue.add(currentClass.getSuperclass());
        }

        return assignableClasses;
    }

    private static boolean hasAnnotation(final InputStream classFileInputStream) {
        try {
            // u4 magic;
            // u2 minor_version;
            // u2 major_version;
            classFileInputStream.skip(8);

            // u2 constant_pool_count;
            final byte[] constant_pool_count_array = new byte[2];
            classFileInputStream.read(constant_pool_count_array);
            final int constant_pool_count = toInt(constant_pool_count_array, 0, 2);

            for (int constantPoolIndex = 1; constantPoolIndex < constant_pool_count; ++constantPoolIndex) {

                final int tag = classFileInputStream.read();

                switch (tag) {
                case 7: // CONSTANT_Class
                    classFileInputStream.skip(2);
                    break;
                case 9: // CONSTANT_Fieldref
                case 10: // CONSTANT_Methodref
                case 11: // CONSTANT_InterfaceMethodref
                    classFileInputStream.skip(4);
                    break;
                case 8: // CONSTANT_String
                    classFileInputStream.skip(2);
                    break;
                case 3: // CONSTANT_Integer
                case 4: // CONSTANT_Float
                    classFileInputStream.skip(4);
                    break;
                case 5: // CONSTANT_Long
                case 6: // CONSTANT_Double
                    classFileInputStream.skip(8);
                    constantPoolIndex++;
                    break;
                case 12: // CONSTANT_NameAndType
                    classFileInputStream.skip(4);
                    break;
                case 1: // CONSTANT_Utf8
                    final byte[] length_array = new byte[2];
                    classFileInputStream.read(length_array);
                    final int length = toInt(length_array, 0, 2);

                    final byte[] utf8_array = new byte[length];
                    classFileInputStream.read(utf8_array);
                    final String utf8 = new String(utf8_array, 0, length);

                    // include classes that contain @UseTool, as well
                    // in order to inject tools into their static fields, if
                    // necessary
                    // Users of Toolbox must understand that:
                    // 1. For classes annotated with @Tool, static field
                    // injection occurs before the method that builds the tool
                    // is called
                    // 2. For un-annotated classes, static field injection
                    // occurs only after all tools have been built.
                    if (TOOL_ANNOTATION_UTF8.equals(utf8) || USE_TOOL_ANNOTATION_UTF8.equals(utf8)) {
                        return true;
                    }
                    break;
                case 15: // CONSTANT_MethodHandle
                    classFileInputStream.skip(3);
                    break;
                case 16: // CONSTANT_MethodType
                    classFileInputStream.skip(2);
                    break;
                case 18: // CONSTANT_InvokeDynamic
                    classFileInputStream.skip(4);
                    break;
                }
            }
        } catch (final IOException e) {
            throw new ToolScannerIOException(String.format("Error parsing bytecode"), e);
        } finally {
            close(classFileInputStream);
        }

        return false;
    }

    private static int toInt(final byte[] bytes, final int start, final int end) {
        int value = 0;
        for (int i = start; i < end; i++) {
            value = (value << 8) + (bytes[i] & 0xff);
        }
        return value;
    }

    private static List<JarEntry> getClassFileJarEntriesUnderBasePackages(final JarFile jarFile,
            final String[] basePackages) {
        final List<JarEntry> classes = new ArrayList<JarEntry>();
        JarInputStream jarInputStream = null;
        try {
            jarInputStream = new JarInputStream(new FileInputStream(jarFile.getName()));
            JarEntry jarEntry = jarInputStream.getNextJarEntry();
            while (jarEntry != null) {
                if (isUnderBasePackage(jarEntry, basePackages) && (jarEntry.getName().endsWith(".class"))) {
                    classes.add(jarEntry);
                }
                jarEntry = jarInputStream.getNextJarEntry();
            }
        } catch (final Exception e) {
            throw new ToolScannerIOException(String.format("Error getting classes from %s", jarFile.getName()), e);
        } finally {
            close(jarInputStream);
        }

        return classes;
    }

    private static boolean isUnderBasePackage(final JarEntry jarEntry, final String[] basePackages) {
        for (final String basePackage : basePackages) {
            if (jarEntry.getName().startsWith(basePackage.replaceAll("\\.", "/"))) {
                return true;
            }
        }
        return false;
    }

    private static void close(final Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (final IOException e) {

        }
    }

    private static boolean isClassAlreadyLoaded(final String className) {
        try {
            final Method method = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            method.setAccessible(true);
            final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            return null != method.invoke(classLoader, className);
        } catch (final Exception e) {
            throw new ReflectionException(String.format("class: %s", className), e);
        }
    }
}
