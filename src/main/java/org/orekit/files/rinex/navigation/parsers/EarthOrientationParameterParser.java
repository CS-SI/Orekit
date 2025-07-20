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
package org.orekit.files.rinex.navigation.parsers;

import org.orekit.files.rinex.navigation.EarthOrientationParameterMessage;
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.utils.units.Unit;

/** Parser for Earth Orientation Parameter.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class EarthOrientationParameterParser
    extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for Earth Orientation Parameter message. */
    private final EarthOrientationParameterMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public EarthOrientationParameterParser(final ParseInfo parseInfo, EarthOrientationParameterMessage message) {
        super(RecordType.ION);
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        message.setReferenceEpoch(parseInfo.parseDate(parseInfo.getLine(), message.getSystem()));
        message.setXp(parseInfo.parseDouble2(Unit.ARC_SECOND));
        message.setXpDot(parseInfo.parseDouble3(RinexNavigationParser.AS_PER_DAY));
        message.setXpDotDot(parseInfo.parseDouble4(RinexNavigationParser.AS_PER_DAY2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setYp(parseInfo.parseDouble2(Unit.ARC_SECOND));
        message.setYpDot(parseInfo.parseDouble3(RinexNavigationParser.AS_PER_DAY));
        message.setYpDotDot(parseInfo.parseDouble4(RinexNavigationParser.AS_PER_DAY2));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setDut1(parseInfo.parseDouble2(Unit.SECOND));
        message.setDut1Dot(parseInfo.parseDouble3(RinexNavigationParser.S_PER_DAY));
        message.setDut1DotDot(parseInfo.parseDouble4(RinexNavigationParser.S_PER_DAY2));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addEarthOrientationParameter(message);
    }

}
