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

package org.toolboxframework.transform;

class Bytecode {
    static final byte[] TOOLBOX_CONSTANTS = {
            (byte) 0x01,
            (byte) 0x00, (byte) 0x28,
            (byte) 'o', (byte) 'r', (byte) 'g', (byte) '/',
            (byte) 't', (byte) 'o', (byte) 'o', (byte) 'l', (byte) 'b', (byte) 'o', (byte) 'x', (byte) 'f', (byte) 'r',
            (byte) 'a', (byte) 'm', (byte) 'e', (byte) 'w', (byte) 'o', (byte) 'r', (byte) 'k', (byte) '/',
            (byte) 'i', (byte) 'n', (byte) 'j', (byte) 'e', (byte) 'c', (byte) 't', (byte) '/',
            (byte) 'T', (byte) 'o', (byte) 'o', (byte) 'l', (byte) 'I', (byte) 'n', (byte) 'j', (byte) 'e', (byte) 'c',
            (byte) 't', (byte) 'o', (byte) 'r',

            (byte) 0x01,
            (byte) 0x00, (byte) 0x0b,
            (byte) 'i', (byte) 'n', (byte) 'j', (byte) 'e', (byte) 'c', (byte) 't', (byte) 'T', (byte) 'o', (byte) 'o',
            (byte) 'l', (byte) 's',

            (byte) 0x01,
            (byte) 0x00, (byte) 0x15,
            (byte) '(',
            (byte) 'L',
            (byte) 'j', (byte) 'a', (byte) 'v', (byte) 'a', (byte) '/',
            (byte) 'l', (byte) 'a', (byte) 'n', (byte) 'g', (byte) '/',
            (byte) 'O', (byte) 'b', (byte) 'j', (byte) 'e', (byte) 'c', (byte) 't', (byte) ';',
            (byte) ')',
            (byte) 'V',

            (byte) 0x07,
            /* 82 */ (byte) 0x00, (byte) 0x00,

            (byte) 0x0c,
            (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00,

            (byte) 0x0a,
            (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00 };

    static final byte[] TOOLBOX_COMMANDS = {
            (byte) 0x2a,
            (byte) 0xb8,
            (byte) 0x00, (byte) 0x00 };

    static final int classNameIndexValuePosition = 82;
    static final int nameAndTypeNameIndexValuePosition = classNameIndexValuePosition + 3;
    static final int nameAndTypeDescriptorIndexValuePosition = nameAndTypeNameIndexValuePosition + 2;
    static final int methodClassIndexValuePosition = nameAndTypeDescriptorIndexValuePosition + 3;
    static final int methodNameAndTypeIndexValuePosition = methodClassIndexValuePosition + 2;
}
