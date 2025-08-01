/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.navigation.parsers.ephemeris;

import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.propagation.analytical.gnss.data.AbstractEphemerisMessage;

/** Parser for abstract ephemeris.
 * @param <T> type of the ephemeris message
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public abstract class AbstractEphemerisParser<T extends AbstractEphemerisMessage> extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for ephemeris message. */
    private final T message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public AbstractEphemerisParser(final ParseInfo parseInfo, final T message) {
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** Get the container for parsing data.
     * @return container for parsing data
     */
    public ParseInfo getParseInfo() {
        return parseInfo;
    }

    /** Get the container for the navigation message.
     * @return container for the navigation message
     */
    public T getMessage() {
        return message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setX(parseInfo.parseDouble1(RinexNavigationParser.KM));
        message.setXDot(parseInfo.parseDouble2(RinexNavigationParser.KM_PER_S));
        message.setXDotDot(parseInfo.parseDouble3(RinexNavigationParser.KM_PER_S2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        message.setY(parseInfo.parseDouble1(RinexNavigationParser.KM));
        message.setYDot(parseInfo.parseDouble2(RinexNavigationParser.KM_PER_S));
        message.setYDotDot(parseInfo.parseDouble3(RinexNavigationParser.KM_PER_S2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        message.setZ(parseInfo.parseDouble1(RinexNavigationParser.KM));
        message.setZDot(parseInfo.parseDouble2(RinexNavigationParser.KM_PER_S));
        message.setZDotDot(parseInfo.parseDouble3(RinexNavigationParser.KM_PER_S2));
    }

}
