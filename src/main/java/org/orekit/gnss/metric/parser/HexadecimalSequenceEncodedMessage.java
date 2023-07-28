/* Copyright 2023 Thales Alenia Space
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
package org.orekit.gnss.metric.parser;

/** Encoded message as an hexadecimal characters sequence.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class HexadecimalSequenceEncodedMessage extends AbstractEncodedMessage {

    /** Hexadecimal radix. */
    private static final int HEXA = 16;

    /** Characters sequence containing the message. */
    private final CharSequence message;

    /** Index of current character in array. */
    private int charIndex;

    /** Simple constructor.
     * @param message characters sequence containing the message
     */
    public HexadecimalSequenceEncodedMessage(final CharSequence message) {
        this.message = message;
    }

    /** {@inheritDoc} */
    @Override
    public void start() {
        super.start();
        this.charIndex = -1;
    }

    /** {@inheritDoc} */
    @Override
    protected int fetchByte() {
        if (charIndex + 2 >= message.length()) {
            return -1;
        }
        final int high = Character.digit(message.charAt(++charIndex), HEXA);
        final int low  = Character.digit(message.charAt(++charIndex), HEXA);
        return high << 4 | low;
    }

}
