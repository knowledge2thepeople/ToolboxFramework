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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.toolboxframework.Toolbox;

class ToolMakerMethodToolBuilder implements ToolBuilder {
    final ToolDescription toolMakerToolDescription;
    final Method method;
    final DependencyDescription[] parameterDependencyDescriptions;

    ToolMakerMethodToolBuilder(final ToolDescription toolMakerToolDescription, final Method method,
            final DependencyDescription[] parameterDependencyDescriptions) {
        this.toolMakerToolDescription = toolMakerToolDescription;
        this.method = method;
        this.parameterDependencyDescriptions = parameterDependencyDescriptions;
    }

    public Object buildTool() {
        final Object[] parameters = new Object[parameterDependencyDescriptions.length];
        for (int i = 0; i < parameters.length; ++i) {
            final DependencyDescription dependencyDescription = parameterDependencyDescriptions[i];
            if (dependencyDescription.explicitName == null) {
                parameters[i] = Toolbox.getTool(dependencyDescription.type);
            } else {
                parameters[i] = Toolbox.getTool(dependencyDescription.explicitName, dependencyDescription.type);
            }
        }
        try {
            method.setAccessible(true);
            if (Modifier.isStatic(method.getModifiers())) {
                return method.invoke(null, parameters);
            } else {
                final Object toolMaker = Toolbox.getTool(toolMakerToolDescription.type, toolMakerToolDescription.name);
                return method.invoke(toolMaker, parameters);
            }
        } catch (final Exception e) {
            throw new UnableToBuildToolException(String.format("method for building tool: %s", method.toString()), e);
        }
    }
}
