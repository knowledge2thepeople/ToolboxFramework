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

import java.lang.instrument.IllegalClassFormatException;

class ClassReader {
    private static final String INIT_UTF8 = "<init>";
    private static final String USE_TOOL_ANNOTATION_UTF8 = "Lorg/toolboxframework/inject/annotation/UseTool;";
    private static final String CODE_UTF8 = "Code";

    public ClassInfo readClass(final byte[] bytes) throws IllegalClassFormatException {
        final ClassInfo classInfo = new ClassInfo();

        int index = 0;

        // u4 magic;
        // u2 minor_version;
        // u2 major_version;
        index += 8;

        // u2 constant_pool_count;
        classInfo.indexOfConstantPoolCount = index;
        classInfo.constant_pool_count = toInt(bytes, index, index + 2);
        index += 2;

        // cp_info constant_pool[constant_pool_count-1];
        index = getInfoFromConstantsPool(bytes, index, classInfo);
        classInfo.indexAfterEndOfConstantPool = index;

        if (!classInfo.hasUseTool) {
            // annotation not present; no need to continue
            return null;
        }

        // u2 access_flags;
        // u2 this_class;
        // u2 super_class;
        index += 6;

        // u2 interfaces_count;
        final int interfaces_count = toInt(bytes, index, index + 2);
        index += 2;

        // u2 interfaces[interfaces_count];
        index += (2 * interfaces_count);

        // u2 fields_count;
        classInfo.fields_count = toInt(bytes, index, index + 2);
        index += 2;

        // field_info fields[fields_count];
        index = processFields(bytes, index, classInfo);

        // u2 methods_count;
        classInfo.methods_count = toInt(bytes, index, index + 2);
        index += 2;

        // method_info methods[methods_count];
        index = stepThroughMethods(bytes, index, classInfo);

        return classInfo;
    }

    private int getInfoFromConstantsPool(final byte[] bytes, int index, final ClassInfo classInfo)
            throws IllegalClassFormatException {
        for (int constantPoolIndex = 1; constantPoolIndex < classInfo.constant_pool_count; ++constantPoolIndex) {
            final int tag = Integer.valueOf(bytes[index]);
            index += 1;

            switch (tag) {
            // CONSTANT_Class
            case 7:
                index += 2;
                break;
            // CONSTANT_Fieldref
            case 9:
                // CONSTANT_Methodref
            case 10:
                // CONSTANT_InterfaceMethodref
            case 11:
                index += 4;
                break;
            // CONSTANT_String
            case 8:
                index += 2;
                break;
            // CONSTANT_Integer 3
            case 3:
            case 4: // CONSTANT_Float 4
                index += 4;
                break;
            case 5: // CONSTANT_Long
            case 6: // CONSTANT_Double
                index += 8;
                constantPoolIndex++;
                break;
            case 12: // CONSTANT_NameAndType
                index += 4;
                break;
            case 1: // CONSTANT_Utf8
                final int length = toInt(bytes, index, index + 2);
                index += 2;

                final String utf8 = new String(bytes, index, length);
                index += length;

                if (INIT_UTF8.equals(utf8)) {
                    classInfo.initUtf8 = constantPoolIndex;
                } else if (CODE_UTF8.equals(utf8)) {
                    classInfo.codeUtf8 = constantPoolIndex;
                } else if (USE_TOOL_ANNOTATION_UTF8.equals(utf8)) {
                    classInfo.hasUseTool = true;
                }
                break;
            // CONSTANT_MethodHandle
            case 15:
                index += 3;
                break;
            // CONSTANT_MethodType
            case 16:
                index += 2;
                break;
            // CONSTANT_InvokeDynamic
            case 18:
                index += 4;
                break;
            }
        }
        return index;
    }

    private int stepThroughMethods(final byte[] bytes, int index, final ClassInfo classInfo) {
        for (int i = 0; i < classInfo.methods_count; ++i) {
            // u2 access_flags;
            index += 2;

            // u2 name_index;
            final int name_index = toInt(bytes, index, index + 2);
            index += 2;

            // u2 descriptor_index;
            index += 2;

            // u2 attributes_count;
            final int indexOfAttributesCount = index;
            final int attributes_count = toInt(bytes, index, index + 2);
            index += 2;

            // we only care about constructors
            if (name_index != classInfo.initUtf8) {
                for (int j = 0; j < attributes_count; ++j) {
                    index = processAttribute(bytes, index, classInfo);
                }
                continue;
            }

            final ConstructorInfo constructorInfo = new ConstructorInfo();
            constructorInfo.attributes_count = attributes_count;
            constructorInfo.indexOfAttributesCount = indexOfAttributesCount;

            classInfo.constructorInfoList.add(constructorInfo);

            index = getInfoFromConstructorMethodAttributes(bytes, index, constructorInfo, classInfo);
        }
        return index;
    }

    private int getInfoFromConstructorMethodAttributes(final byte[] bytes,
            int index,
            final ConstructorInfo constructorInfo,
            final ClassInfo classInfo) {

        for (int i = 0; i < constructorInfo.attributes_count; ++i) {
            // u2 attribute_name_index
            final int attribute_name_index = toInt(bytes, index, index + 2);
            index += 2;

            // u4 attribute_length
            final int indexOfAttributeLength = index;
            final int attribute_length = toInt(bytes, index, index + 4);
            index += 4;

            if (attribute_name_index == classInfo.codeUtf8) {
                int localIndex = index;

                // u2 max_stack;
                // u2 max_locals;
                localIndex += 4;

                // u4 code_length;
                final int indexOfCodeLength = localIndex;
                final int code_length = toInt(bytes, indexOfCodeLength, indexOfCodeLength + 4);
                localIndex += 4;

                // u1 code[code_length];
                final int indexOfStartOfCode = localIndex;

                final int indexAfterFirstInvokeSpecial = getIndexAfterFirstInvokeSpecial(bytes,
                        indexOfStartOfCode, indexOfStartOfCode + code_length);

                final CodeInfo codeInfo = new CodeInfo();
                codeInfo.indexOfAttributeLength = indexOfAttributeLength;
                codeInfo.attribute_length = attribute_length;
                codeInfo.indexOfCodeLength = indexOfCodeLength;
                codeInfo.code_length = code_length;
                codeInfo.indexAfterFirstInvokeSpecial = indexAfterFirstInvokeSpecial;

                constructorInfo.codeInfo = codeInfo;
            }

            // u1 info[attribute_length]
            index += attribute_length;
        }

        return index;
    }

    private int getIndexAfterFirstInvokeSpecial(final byte[] bytes, final int index, final int upperBound) {
        final byte invokeSpecial = (byte) 0xb7; // invoke special
        int i = index;
        boolean found = false;
        while (!found && i < upperBound) {
            if (bytes[i] == invokeSpecial) {
                found = true;
            }
            i++;
        }
        if (!found) {
            throw new RuntimeException("Call to \"invoke special\" not found in constructor!");
        }

        return i + 2;
    }

    private int processFields(final byte[] bytes, int index, final ClassInfo classInfo) {
        for (int i = 0; i < classInfo.fields_count; ++i) {
            // u2 access_flags;
            // u2 name_index;
            // u2 descriptor_index;
            index += 6;

            // u2 attributes_count;
            final int attributes_count = toInt(bytes, index, index + 2);
            index += 2;

            // attribute_info attributes[attributes_count];
            for (int j = 0; j < attributes_count; ++j) {
                index = processAttribute(bytes, index, classInfo);
            }
        }

        return index;
    }

    private int processAttribute(final byte[] bytes, int index, final ClassInfo classInfo) {
        // attribute_name_index
        index += 2;

        // attribute_length
        final int attribute_length = toInt(bytes, index, index + 4);
        index += 4;

        return index + attribute_length;
    }

    private int toInt(final byte[] bytes, final int start, final int end) {
        int value = 0;
        for (int i = start; i < end; i++) {
            value = (value << 8) + (bytes[i] & 0xff);
        }
        return value;
    }
}
