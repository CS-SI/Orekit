/* Copyright 2002-2023 CS GROUP
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

/** Interface for getting bits forming encoded messages.
 * <p>
 * Classes implementing this interface must contain exactly one complete message.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface EncodedMessage {

    /** Start message extraction.
     */
    default void start() {
        // nothing by default
    }

    /** Extract the next n bits from the encoded message.
     * @param n number of bits to extract (cannot exceed 32 bits)
     * @return bits packed as the LSB of a 64 bits primitive long
     */
    long extractBits(int n);

}
