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
import org.orekit.files.rinex.navigation.RecordType;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.RecordLineParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.propagation.analytical.gnss.data.NavICLegacyNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for NavIC legacy.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class NavICLnavParser extends RecordLineParser {

    /** URA index to URA mapping (table 23 of NavIC ICD). */
    // CHECKSTYLE: stop Indentation check
    static final double[] NAVIC_URA = {
           2.40,    3.40,    4.85,   6.85,
           9.65,   13.65,   24.00,  48.00,
          96.00,  192.00,  384.00, 768.00,
        1536.00, 3072.00, 6144.00, Double.NaN
    };
    // CHECKSTYLE: resume Indentation check

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for navigation message. */
    private final NavICLegacyNavigationMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public NavICLnavParser(final ParseInfo parseInfo, final NavICLegacyNavigationMessage message) {
        super(RecordType.ORBIT);
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getNavIC(), parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        message.setIODE(parseInfo.parseInt1());
        message.setIODC(message.getIODE());
        message.setCrs(parseInfo.parseDouble2(Unit.METRE));
        message.setDeltaN0(parseInfo.parseDouble3(RinexNavigationParser.RAD_PER_S));
        message.setM0(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine02() {
        message.setCuc(parseInfo.parseDouble1(Unit.RADIAN));
        message.setE(parseInfo.parseDouble2(Unit.NONE));
        message.setCus(parseInfo.parseDouble3(Unit.RADIAN));
        message.setSqrtA(parseInfo.parseDouble4(RinexNavigationParser.SQRT_M));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine03() {
        message.setTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setCic(parseInfo.parseDouble2(Unit.RADIAN));
        message.setOmega0(parseInfo.parseDouble3(Unit.RADIAN));
        message.setCis(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine04() {
        message.setI0(parseInfo.parseDouble1(Unit.RADIAN));
        message.setCrc(parseInfo.parseDouble2(Unit.METRE));
        message.setPa(parseInfo.parseDouble3(Unit.RADIAN));
        message.setOmegaDot(parseInfo.parseDouble4(RinexNavigationParser.RAD_PER_S));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        message.setIDot(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        message.setL2Codes(parseInfo.parseInt2());
        message.setWeek(parseInfo.parseInt3());
        message.setL2PFlags(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final int uraIndex = parseInfo.parseInt1();
        message.setSvAccuracy(NAVIC_URA[FastMath.min(uraIndex, NAVIC_URA.length - 1)]);
        message.setSvHealth(parseInfo.parseInt2());
        message.setTGD(parseInfo.parseDouble3(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addNavICLegacyNavigationMessage(message);
    }

}
