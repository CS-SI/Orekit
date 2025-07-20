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
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.utils.units.Unit;

/** Parser for Beidou legacy.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouD1D2Parser extends SatelliteSystemLineParser {

    /** Container for parsing data. */
    private final ParseInfo parseInfo;

    /** Container for navigation message. */
    private final BeidouLegacyNavigationMessage message;

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    BeidouD1D2Parser(final ParseInfo parseInfo, final BeidouLegacyNavigationMessage message) {
        this.parseInfo = parseInfo;
        this.message   = message;
    }

    /** {@inheritDoc} */
    @Override
    public void parseSvEpochSvClockLine() {
        parseSvEpochSvClockLine(parseInfo.getLine(), parseInfo.getTimeScales().getBDT(),
                                parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseFirstBroadcastOrbit() {
        message.setAODE(parseInfo.parseDouble1(Unit.SECOND));
        message.setCrs(parseInfo.parseDouble2(Unit.METRE));
        message.setDeltaN0(parseInfo.parseDouble3(RinexNavigationParser.RAD_PER_S));
        message.setM0(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseSecondBroadcastOrbit() {
        message.setCuc(parseInfo.parseDouble1(Unit.RADIAN));
        message.setE(parseInfo.parseDouble2(Unit.NONE));
        message.setCus(parseInfo.parseDouble3(Unit.RADIAN));
        message.setSqrtA(parseInfo.parseDouble4(RinexNavigationParser.SQRT_M));
    }

    /** {@inheritDoc} */
    @Override
    public void parseThirdBroadcastOrbit() {
        message.setTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setCic(parseInfo.parseDouble2(Unit.RADIAN));
        message.setOmega0(parseInfo.parseDouble3(Unit.RADIAN));
        message.setCis(parseInfo.parseDouble4(Unit.RADIAN));
    }

    /** {@inheritDoc} */
    @Override
    public void parseFourthBroadcastOrbit() {
        message.setI0(parseInfo.parseDouble1(Unit.RADIAN));
        message.setCrc(parseInfo.parseDouble2(Unit.METRE));
        message.setPa(parseInfo.parseDouble3(Unit.RADIAN));
        message.setOmegaDot(parseInfo.parseDouble4(RinexNavigationParser.RAD_PER_S));
    }

    /** {@inheritDoc} */
    @Override
    public void parseFifthBroadcastOrbit() {
        // iDot
        message.setIDot(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        // BDT week (to go with Toe)
        message.setWeek(parseInfo.parseInt3());
    }

    /** {@inheritDoc} */
    @Override
    public void parseSixthBroadcastOrbit() {
        message.setSvAccuracy(parseInfo.parseDouble1(Unit.METRE));
        message.setSatH1(parseInfo.parseInt2());
        message.setTGD1(parseInfo.parseDouble3(Unit.SECOND));
        message.setTGD2(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseSeventhBroadcastOrbit() {
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        message.setAODC(parseInfo.parseDouble2(Unit.SECOND));
        parseInfo.closePendingMessage();
    }

    /** {@inheritDoc} */
    @Override
    public void closeMessage(final RinexNavigation file) {
        file.addBeidouLegacyNavigationMessage(message);
    }

}
