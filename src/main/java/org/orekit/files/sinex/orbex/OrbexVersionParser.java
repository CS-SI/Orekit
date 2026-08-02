/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.orekit.files.sinex.LineParser;
import org.orekit.files.sinex.ParseInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for Orbex version line.
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class OrbexVersionParser implements LineParser<OrbexParseInfo> {

    /** Pattern for version line. */
    private final Pattern pattern;

    /** Simple constructor.
     */
    protected OrbexVersionParser() {
        pattern = Pattern.compile("%=" + "ORBEX" + " \\d\\.\\d\\d .+");
    }

    /** {@inheritDoc} */
    @Override
    public boolean parseIfRecognized(final OrbexParseInfo parseInfo) {
        final Matcher matcher = pattern.matcher(parseInfo.getLine());
        if (matcher.matches()) {
            // we have recognized an ORBEX file first line
            // parse the various dates it contains
            parseInfo.setCreationDate(matcher.group(1));
            return true;
        } else {
            // this is not an expected OREBEX file
            return false;
        }
    }

}
