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

import org.orekit.gnss.SatInSystem;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Predicate for satellite id and description blocks.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SatIdAndDescriptionPredicate implements Predicate<OrbexParseInfo> {

    /** Pattern for id and description lines. */
    private static final Pattern ID_AND_DESC_PATTERN =
        Pattern.compile("^ (\\p{Alpha}\\p{Digit}{2}) {4}(.*)\\p{Blank}*$");

    /** {@inheritDoc} */
    @Override
    public boolean test(final OrbexParseInfo parseInfo) {
        final Matcher matcher = ID_AND_DESC_PATTERN.matcher(parseInfo.getLine());
        if (matcher.matches()) {
            // this is the data type we are concerned with
            parseInfo.addSatIdAndDescription(new SatInSystem(matcher.group(1)), matcher.group(2));
            return true;
        } else {
            // it is a data type for another predicate
            return false;
        }
    }
}
