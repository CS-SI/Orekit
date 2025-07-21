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

import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.propagation.analytical.gnss.data.GLONASSFdmaNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

/** Parser for Glonass FDMA.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GlonassFdmaParser extends AbstractEphemerisParser<GLONASSFdmaNavigationMessage> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public GlonassFdmaParser(final ParseInfo parseInfo, final GLONASSFdmaNavigationMessage message) {
        super(parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {

        final ParseInfo parseInfo = getParseInfo();
        final GLONASSFdmaNavigationMessage message = getMessage();

        if (parseInfo.getHeader().getFormatVersion() < 3.0) {

            message.setPRN(ParsingUtils.parseInt(parseInfo.getLine(), 0, 2));

            // Toc
            final int year = ParsingUtils.convert2DigitsYear(ParsingUtils.parseInt(parseInfo.getLine(), 3, 2));
            final int month = ParsingUtils.parseInt(parseInfo.getLine(), 6, 2);
            final int day = ParsingUtils.parseInt(parseInfo.getLine(), 9, 2);
            final int hours = ParsingUtils.parseInt(parseInfo.getLine(), 12, 2);
            final int min = ParsingUtils.parseInt(parseInfo.getLine(), 15, 2);
            final double sec = ParsingUtils.parseDouble(parseInfo.getLine(), 17, 5);
            message.setEpochToc(new AbsoluteDate(year, month, day, hours, min, sec,
                                                 parseInfo.getTimeScales().getUTC()));

            // clock
            message.setTauN(Unit.SECOND.toSI(-ParsingUtils.parseDouble(parseInfo.getLine(), 22, 19)));
            message.setGammaN(Unit.NONE.toSI(ParsingUtils.parseDouble(parseInfo.getLine(), 41, 19)));

            // Set the ephemeris epoch (same as time of clock epoch)
            message.setDate(message.getEpochToc());

        } else {
            message.setPRN(ParsingUtils.parseInt(parseInfo.getLine(), 1, 2));

            // Toc
            message.setEpochToc(parseInfo.parseDate(parseInfo.getTimeScales().getUTC()));

            // clock
            message.setTauN(-parseInfo.parseDouble2(Unit.ONE));
            message.setGammaN(parseInfo.parseDouble3(Unit.ONE));
            message.setTime(fmod(parseInfo.parseDouble4(Unit.ONE), Constants.JULIAN_DAY));

            // Set the ephemeris epoch (same as time of clock epoch)
            message.setDate(message.getEpochToc());
        }

    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        getMessage().setHealth(getParseInfo().parseDouble4(Unit.NONE));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        super.parseLine02();
        getMessage().setFrequencyNumber(getParseInfo().parseDouble4(Unit.NONE));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        super.parseLine03();
        final ParseInfo parseInfo = getParseInfo();
        if (parseInfo.getHeader().getFormatVersion() < 3.045) {
            parseInfo.closePendingRecord();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine04() {
        final ParseInfo parseInfo = getParseInfo();
        final GLONASSFdmaNavigationMessage message = getMessage();
        message.setStatusFlags(parseInfo.parseDouble1(Unit.NONE));
        message.setGroupDelayDifference(parseInfo.parseDouble2(Unit.NONE));
        message.setURA(parseInfo.parseDouble3(Unit.NONE));
        message.setHealthFlags(parseInfo.parseDouble4(Unit.NONE));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addGlonassNavigationMessage(getMessage());
    }

}
