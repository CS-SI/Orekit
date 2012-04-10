/* Copyright 2002-2012 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.utils;


/** Utility class for setting global configuration parameters.

 * @author Luc Maisonobe
 */
public class OrekitConfiguration {

    /** Default maximum number of slots to use in caches. */
    private static int DEFAULT_MAX_SLOTS_NUMBER;

    static {
        DEFAULT_MAX_SLOTS_NUMBER = 100;
    }

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private OrekitConfiguration() {
    }

    /** Set the default maximum number of slots to use in caches.
     * @param default maximum number of slots to use in caches
     */
    public static void setDefaultMaxSlotsNumber(final int defaultMaxSlotsNumber) {
        OrekitConfiguration.DEFAULT_MAX_SLOTS_NUMBER = defaultMaxSlotsNumber;
    }

    /** Get the default maximum number of slots to use in caches.
     * @return default maximum number of slots to use in caches
     */
    public static int getDefaultMaxSlotsNumber() {
        return DEFAULT_MAX_SLOTS_NUMBER;
    }

}
