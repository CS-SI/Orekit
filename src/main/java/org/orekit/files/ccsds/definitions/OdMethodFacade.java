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

/** Facade in front of several orbit determination methods in CCSDS messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OdMethodFacade {

    /** Pattern for splitting string specification in OCM files. */
    private static final Pattern SPLITTER = Pattern.compile("\\p{Blank}*:\\p{Blank}*");

    /** Name of the method. */
    private final String name;

    /** Method type (may be null). */
    private final OdMethodType type;

    /** Tool used for OD. */
    private final String tool;

    /** Simple constructor.
     * @param name name of the method
     * @param type method type (may be null)
     * @param tool tool used for OD (may be null)
     */
    public OdMethodFacade(final String name, final OdMethodType type, final String tool) {
        this.name = name;
        this.type = type;
        this.tool = tool;
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
    public OdMethodType getType() {
        return type;
    }

    /** Get the tool used for OD.
     * @return tool used for OD
     */
    public String getTool() {
        return tool;
    }

    /** Parse a string from OCM.
     * @param s string to parse
     * @return OD method facade
     */
    public static OdMethodFacade parse(final String s) {
        final String[] fields = SPLITTER.split(s);
        if (fields.length == 2) {
            // we have method and tool
            OdMethodType type;
            try {
                type = OdMethodType.valueOf(fields[0]);
            } catch (IllegalArgumentException iae) {
                type = null;
            }
            return new OdMethodFacade(fields[0], type, fields[1]);
        } else {
            return new OdMethodFacade(s, null, null);
        }
    }

}
