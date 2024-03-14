/* Copyright 2002-2024 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.time;

/** Abstract base class for CCSDS segmented and unsegmented time code.
 * @author Luc Maisonobe
 * @since 12.1
 * @see AbsoluteDate
 * @see FieldAbsoluteDate
 */
abstract class AbstractCcsdsTimeCode {

    /** Decode a signed byte as an unsigned int value.
     * @param b byte to decode
     * @return an unsigned int value
     */
    protected int toUnsigned(final byte b) {
        final int i = (int) b;
        return (i < 0) ? 256 + i : i;
    }

    /** Format a byte as an hex string for error messages.
     * @param data byte to format
     * @return a formatted string
     */
    protected String formatByte(final byte data) {
        return "0x" + Integer.toHexString(data).toUpperCase();
    }

}
