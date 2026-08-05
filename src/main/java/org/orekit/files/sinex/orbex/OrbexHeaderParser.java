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

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parser for Orbex two-line header.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class OrbexHeaderParser implements LineParser<OrbexParseInfo> {

    /** Pattern for version line. */
    private final Pattern pattern;

    /** Top level parsers. */
    private final List<LineParser<OrbexParseInfo>> topParsers;

    /** Simple constructor.
     * @param topParsers top level parsers
     */
    protected OrbexHeaderParser(final List<LineParser<OrbexParseInfo>> topParsers) {
        this.pattern    = Pattern.compile("^%=ORBEX\\p{Blank}+([0-9.]+).*$");
        this.topParsers = topParsers;
    }

    /** {@inheritDoc} */
    @Override
    public boolean parseIfRecognized(final OrbexParseInfo parseInfo) {
        return switch (parseInfo.getLineNumber()) {
            case 1 -> {
                final Matcher matcher = pattern.matcher(parseInfo.getLine());
                if (matcher.matches()) {
                    // we have recognized an ORBEX file first line
                    // parse the version number
                    parseInfo.setVersion(Double.parseDouble(matcher.group(1)));
                    yield true;
                } else {
                    // this is not an expected OREBEX file
                    yield false;
                }
            }
            case 2  -> parseInfo.getLine().startsWith("%%"); // we allow missing blank character
            default -> false;
        };
    }

    /** {@inheritDoc} */
    @Override
    public Iterable<LineParser<OrbexParseInfo>> allowedNextParsers(final OrbexParseInfo parseInfo) {
        return parseInfo.getLineNumber() == 1 ?
               Collections.singleton(this) :
               topParsers;
    }

}
