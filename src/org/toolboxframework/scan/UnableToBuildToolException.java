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

/**
 * This exception is raised if an exception occurs during the construction of a
 * tool during a scan.
 * <p>
 * This exception is a {@link RuntimeException} because it is exposed to the
 * client. Using a {@link RuntimeException} avoids bad coding practices on the
 * client side where they catch the exception and do nothing.
 *
 * @author T.C.C.
 * @version 1.0
 */
public class UnableToBuildToolException extends ToolScannerException {
    private static final long serialVersionUID = 1L;

    public UnableToBuildToolException(final String message) {
        super(message);
    }

    public UnableToBuildToolException(final String message, final Throwable e) {
        super(message, e);
    }

    public UnableToBuildToolException(final Throwable e) {
        super(e);
    }
}
