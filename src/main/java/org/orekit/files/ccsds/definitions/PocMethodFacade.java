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
package org.orekit.files.ccsds.definitions;

import java.util.regex.Pattern;

/** Facade in front of several probability of collision methods in CCSDS messages.
 * @author Bryan Cazabonne
 * @since 11.2
 */
public class PocMethodFacade {

    /** Pattern for POC method names. */
    private static final Pattern PATTERN = Pattern.compile("-");

    /** Name of the method. */
    private final String name;

    /** Method type (may be null). */
    private final PocMethodType type;

    /** Simple constructor.
     * @param name name of the method
     * @param type method type (may be null)
     */
    public PocMethodFacade(final String name, final PocMethodType type) {
        this.name = name;
        this.type = type;
    }

    /** Get the name of the method.
     * @return name of the method
     */
    public String getName() {
        return name;
    }

    /** Get the method type.
     * @return method type
     */
    public PocMethodType getType() {
        return type;
    }

    /** Parse a string from CDM.
     * @param s string to parse
     * @return PoC method facade
     */
    public static PocMethodFacade parse(final String s) {
        PocMethodType type;
        try {
            type = PocMethodType.valueOf(PATTERN.matcher(s).replaceAll("_"));
        } catch (IllegalArgumentException iae) {
            type = null;
        }
        return new PocMethodFacade(s, type);
    }

}
