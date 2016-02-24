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

import java.util.List;

class ToolDescription {
    final Class<?> type;
    final String name;
    final List<DependencyDescription> dependencies;
    final ToolBuilder builder;

    ToolDescription(final Class<?> type, final String name, final List<DependencyDescription> dependencies,
            final ToolBuilder builder) {
        this.type = type;
        this.name = name;
        this.dependencies = dependencies;
        this.builder = builder;
    }

    @Override
    public boolean equals(final Object object) {
        if (null == object || !(object instanceof ToolDescription)) {
            return false;
        }

        final ToolDescription other = (ToolDescription) object;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
