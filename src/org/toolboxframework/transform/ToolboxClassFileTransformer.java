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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Comparator;

class ToolboxClassFileTransformer implements ClassFileTransformer {
    private final ClassReader classReader;
    private final String[] basePackages;

    ToolboxClassFileTransformer(final String args) {
        final String normalized = args.trim().replaceAll("\\.", "/");
        basePackages = normalized.split("[^\\w\\d/]+");

        Arrays.sort(basePackages, new Comparator<String>() {
            @Override
            public int compare(final String a, final String b) {
                return a.length() - b.length();
            }
        });

        classReader = new ClassReader();
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {

        if (shouldTransform(className)) {
            try {
                return transformClassForToolInjection(classfileBuffer);
            } catch (final IllegalClassFormatException e) {
                throw e;
            } catch (final Exception e) {
                throw new TransformException(String.format("class: %s", className), e);
            }
        }

        return classfileBuffer;
    }

    private boolean shouldTransform(final String className) {
        for (final String basePackage : basePackages) {
            if (className.startsWith(basePackage)) {
                return true;
            }
        }
        return false;
    }

    private byte[] transformClassForToolInjection(final byte[] bytes) throws IllegalClassFormatException {
        final ClassInfo classInfo = classReader.readClass(bytes);
        if (classInfo == null) {
            return bytes;
        }

        // calculate length of new byte array
        final int length = bytes.length
                + Bytecode.TOOLBOX_CONSTANTS.length
                + (classInfo.constructorInfoList.size() * Bytecode.TOOLBOX_COMMANDS.length);

        final byte[] newBytes = new byte[length];

        // copy over the first part of the class file
        System.arraycopy(bytes, 0,
                newBytes, 0,
                classInfo.indexAfterEndOfConstantPool);

        // increase the constant pool count
        writeNumberAsTwoBytesToArray(newBytes,
                classInfo.constant_pool_count + 6,
                classInfo.indexOfConstantPoolCount);

        // copy over the toolbox constants
        System.arraycopy(Bytecode.TOOLBOX_CONSTANTS, 0,
                newBytes, classInfo.indexAfterEndOfConstantPool,
                Bytecode.TOOLBOX_CONSTANTS.length);

        // set the toolbox constant references
        writeNumberAsTwoBytesToArray(newBytes,
                classInfo.constant_pool_count,
                classInfo.indexAfterEndOfConstantPool + Bytecode.classNameIndexValuePosition);

        writeNumberAsTwoBytesToArray(newBytes,
                classInfo.constant_pool_count + 1,
                classInfo.indexAfterEndOfConstantPool + Bytecode.nameAndTypeNameIndexValuePosition);

        writeNumberAsTwoBytesToArray(newBytes,
                classInfo.constant_pool_count + 2,
                classInfo.indexAfterEndOfConstantPool + Bytecode.nameAndTypeDescriptorIndexValuePosition);

        writeNumberAsTwoBytesToArray(newBytes,
                classInfo.constant_pool_count + 3,
                classInfo.indexAfterEndOfConstantPool + Bytecode.methodClassIndexValuePosition);

        writeNumberAsTwoBytesToArray(newBytes,
                classInfo.constant_pool_count + 4,
                classInfo.indexAfterEndOfConstantPool + Bytecode.methodNameAndTypeIndexValuePosition);

        // initialize offset to length of the injected constants
        int offset = Bytecode.TOOLBOX_CONSTANTS.length;
        int positionInOriginalBytes = classInfo.indexAfterEndOfConstantPool;

        for (final ConstructorInfo constructorInfo : classInfo.constructorInfoList) {
            final CodeInfo codeInfo = constructorInfo.codeInfo;
            // copy the rest of the bytes until the next insertion point from
            // the source array
            // using the offset to get the insertion point in the destination
            // array
            System.arraycopy(bytes, positionInOriginalBytes,
                    newBytes, positionInOriginalBytes + offset,
                    codeInfo.indexAfterFirstInvokeSpecial - positionInOriginalBytes);

            // copy over the commands
            System.arraycopy(Bytecode.TOOLBOX_COMMANDS, 0,
                    newBytes, codeInfo.indexAfterFirstInvokeSpecial + offset,
                    Bytecode.TOOLBOX_COMMANDS.length);

            // set the commands references
            writeNumberAsTwoBytesToArray(newBytes,
                    classInfo.constant_pool_count + 5,
                    codeInfo.indexAfterFirstInvokeSpecial + offset + 2);

            // update the counts in the destination array, using the offset
            writeNumberAsFourBytesToArray(newBytes,
                    codeInfo.attribute_length + Bytecode.TOOLBOX_COMMANDS.length,
                    codeInfo.indexOfAttributeLength + offset);
            writeNumberAsFourBytesToArray(newBytes,
                    codeInfo.code_length + Bytecode.TOOLBOX_COMMANDS.length,
                    codeInfo.indexOfCodeLength + offset);

            // update positions
            offset += Bytecode.TOOLBOX_COMMANDS.length;
            positionInOriginalBytes = codeInfo.indexAfterFirstInvokeSpecial;
        }

        // copy the rest of the original bytes
        System.arraycopy(bytes, positionInOriginalBytes,
                newBytes, positionInOriginalBytes + offset,
                bytes.length - positionInOriginalBytes);

        return newBytes;
    }

    private void writeNumberAsTwoBytesToArray(final byte[] bytes, final int number, final int startIndex) {
        bytes[startIndex] = (byte) ((number >> 8) & 0xFF);
        bytes[startIndex + 1] = (byte) (number & 0xFF);
    }

    private void writeNumberAsFourBytesToArray(final byte[] bytes, final int number, final int startIndex) {
        bytes[startIndex] = (byte) ((number >> 16) & 0xFF);
        bytes[startIndex + 1] = (byte) ((number >> 12) & 0xFF);
        bytes[startIndex + 2] = (byte) ((number >> 8) & 0xFF);
        bytes[startIndex + 3] = (byte) (number & 0xFF);
    }
}
