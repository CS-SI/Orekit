/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.sinex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Base class for version line.
 * @param <T> type of the SINEX files parse info
 * @author Luc Maisonobe
 * @since 13.0
 */
abstract class VersionParser<T extends ParseInfo<?>> implements LineParser<T> {

    /** Pattern for version line. */
    private final Pattern pattern;

    /** Simple constructor.
     * @param key file format key
     */
    protected VersionParser(final String key) {
        pattern = Pattern.compile("%=" + key + " \\d\\.\\d\\d .+" +
                                  " (\\d{2,4}:\\d{3}:\\d{5}) .+" +
                                  " (\\d{2,4}:\\d{3}:\\d{5}) (\\d{2,4}:\\d{3}:\\d{5})" +
                                  " . .*");
    }

    /** {@inheritDoc} */
    @Override
    public boolean parseIfRecognized(final T parseInfo) {
        final Matcher matcher = pattern.matcher(parseInfo.getLine());
        if (matcher.matches()) {
            // we have recognized a SINEX file first line
            // parse the various dates it contains
            parseInfo.setCreationDate(matcher.group(1));
            parseInfo.setStartDateIfEarlier(matcher.group(2));
            parseInfo.setEndDateIfLater(matcher.group(3));
            return true;
        } else {
            // this is not an expected SINEX file
            return false;
        }
    }

}
