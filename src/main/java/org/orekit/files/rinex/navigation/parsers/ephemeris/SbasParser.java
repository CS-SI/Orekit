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

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.TimeScale;
import org.orekit.utils.units.Unit;

/** Parser for SBAS.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class SbasParser extends AbstractEphemerisParser<SBASNavigationMessage> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public SbasParser(final ParseInfo parseInfo, final SBASNavigationMessage message) {
        super(parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {

        final ParseInfo parseInfo = getParseInfo();
        final SBASNavigationMessage message = getMessage();

        // parse PRN
        message.setPRN(ParsingUtils.parseInt(parseInfo.getLine(), 1, 2));

        // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
        final int version100 = (int) FastMath.rint(parseInfo.getHeader().getFormatVersion() * 100);
        final TimeScale timeScale = (version100 == 301) ?
                                    parseInfo.getTimeScales().getUTC() :
                                    parseInfo.getTimeScales().getGPS();

        message.setEpochToc(parseInfo.parseDate(timeScale));
        message.setAGf0(parseInfo.parseDouble2(Unit.SECOND));
        message.setAGf1(parseInfo.parseDouble3(RinexNavigationParser.S_PER_S));
        message.setTime(parseInfo.parseDouble4(Unit.SECOND));

        // Set the ephemeris epoch (same as time of clock epoch)
        message.setDate(message.getEpochToc());

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
        getMessage().setURA(getParseInfo().parseDouble4(Unit.NONE));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        super.parseLine03();
        final ParseInfo parseInfo = getParseInfo();
        final SBASNavigationMessage message = getMessage();
        message.setIODN(parseInfo.parseDouble4(Unit.NONE));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addSBASNavigationMessage(getMessage());
    }

}
