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

import org.toolboxframework.inject.annotation.UseTool;
import org.toolboxframework.scan.annotation.Tool;
import org.toolboxframework.scan.annotation.ToolMaker;

@ToolMaker
public class StaticMethodsDontDependOnInstanceFieldsToolMaker {
    // the purpose of this test is to prove that C doesn't depend on A
    @UseTool("A")
    private A a;

    @Tool("C")
    private static C message() {
        return new C();
    }
}