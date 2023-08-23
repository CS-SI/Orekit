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

import org.orekit.errors.OrekitInternalError;

/**
 * Interface for data fields used to parsed encoded messages.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public interface DataField {

    /** Get the value of the field as a boolean.
     * @param message message containing the data
     * @return boolean value of the field
     */
    default boolean booleanValue(EncodedMessage message) {
        // this method should be overwritten
        throw new OrekitInternalError(null);
    }

    /** Get the value of the field as an integer.
     * @param message message containing the data
     * @return integer value of the field
     */
    default int intValue(EncodedMessage message) {
        // this method should be overwritten
        throw new OrekitInternalError(null);
    }

    /** Get the value of the field as a double.
     * @param message message containing the data
     * @return double value of the field
     */
    default double doubleValue(EncodedMessage message) {
        // this method should be overwritten
        throw new OrekitInternalError(null);
    }

    /** Get the value of the field as a String.
     * @param message message containing the data
     * @param n number of UTF8 characters
     * @return String value of the field
     */
    default String stringValue(EncodedMessage message, int n) {
        // this method should be overwritten
        throw new OrekitInternalError(null);
    }

}
