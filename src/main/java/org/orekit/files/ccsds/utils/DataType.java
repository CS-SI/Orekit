/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.utils;

/** Raw data types that can be used for entries values.
 * @author Luc Maisonobe
 * @since 11.0
 */
public enum DataType {

    /** No data at all. */
    VOID,

    /** Free text. */
    FREE_TEXT,

    /** Real number. */
    REAL,

    /** Integer number. */
    INT,

    /** List of strings. */
    TEXT_LIST,

    /** Calendar date. */
    CALENDAR_DATE;

}
