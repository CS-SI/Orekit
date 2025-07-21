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

import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.SbasId;
import org.orekit.files.rinex.navigation.SystemTimeOffsetMessage;
import org.orekit.files.rinex.navigation.UtcId;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.utils.units.Unit;

/** Parser for system time offset.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SystemTimeOffsetParser extends RecordLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for system time offset message. */
    private final SystemTimeOffsetMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public SystemTimeOffsetParser(final ParseInfo parseInfo, final SystemTimeOffsetMessage message) {
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {

        final String defTS = ParsingUtils.parseString(parseInfo.getLine(), 24, 2);
        final String refTS = ParsingUtils.parseString(parseInfo.getLine(), 26, 2);
        final String sbas  = ParsingUtils.parseString(parseInfo.getLine(), 43, 18);
        final String utc   = ParsingUtils.parseString(parseInfo.getLine(), 62, 18);

        message.setDefinedTimeSystem(PredefinedTimeSystem.parseTwoLettersCode(defTS));
        message.setReferenceTimeSystem(PredefinedTimeSystem.parseTwoLettersCode(refTS));
        message.setSbasId(sbas == null || sbas.isEmpty() ? null : SbasId.valueOf(sbas));
        message.setUtcId(utc == null || utc.isEmpty() ? null : UtcId.parseUtcId(utc));
        message.setReferenceEpoch(parseInfo.parseDate(message.getSystem()));

    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setA0(parseInfo.parseDouble2(Unit.SECOND));
        message.setA1(parseInfo.parseDouble3(RinexNavigationParser.S_PER_S));
        message.setA2(parseInfo.parseDouble4(RinexNavigationParser.S_PER_S2));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addSystemTimeOffset(message);
    }

}
