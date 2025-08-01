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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.navigation.RinexNavigation;
import org.orekit.files.rinex.navigation.RinexNavigationParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouSatelliteType;
import org.orekit.utils.units.Unit;

/** Parser for Beidou-3 CNAV.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouCnv123Parser extends AbstractNavigationParser<BeidouCivilianNavigationMessage> {

    /** Simple constructor.
     * @param parseInfo container for parsing data
     * @param message container for navigation message
     */
    public BeidouCnv123Parser(final ParseInfo parseInfo, final BeidouCivilianNavigationMessage message) {
        super(parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine00() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        parseSvEpochSvClockLine(parseInfo.getTimeScales().getBDT(), parseInfo, message);
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine01() {
        super.parseLine01();
        getMessage().setADot(getParseInfo().parseDouble1(RinexNavigationParser.M_PER_S));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine05() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        message.setIDot(parseInfo.parseDouble1(RinexNavigationParser.RAD_PER_S));
        message.setDeltaN0Dot(parseInfo.parseDouble2(RinexNavigationParser.RAD_PER_S2));
        switch (parseInfo.parseInt3()) {
            case 0:
                message.setSatelliteType(BeidouSatelliteType.RESERVED);
                break;
            case 1:
                message.setSatelliteType(BeidouSatelliteType.GEO);
                break;
            case 2:
                message.setSatelliteType(BeidouSatelliteType.IGSO);
                break;
            case 3:
                message.setSatelliteType(BeidouSatelliteType.MEO);
                break;
            default:
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          parseInfo.getLineNumber(), parseInfo.getName(),
                                          parseInfo.getLine());
        }
        message.setTime(parseInfo.parseDouble4(Unit.SECOND));
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine06() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        message.setSisaiOe(parseInfo.parseInt1());
        message.setSisaiOcb(parseInfo.parseInt2());
        message.setSisaiOc1(parseInfo.parseInt3());
        message.setSisaiOc2(parseInfo.parseInt4());
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine07() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        if (message.getRadioWave().closeTo(PredefinedGnssSignal.B1C)) {
            message.setIscB1CD(parseInfo.parseDouble1(Unit.SECOND));
            // field 2 is spare
            message.setTgdB1Cp(parseInfo.parseDouble3(Unit.SECOND));
            message.setTgdB2ap(parseInfo.parseDouble4(Unit.SECOND));
        } else if (message.getRadioWave().closeTo(PredefinedGnssSignal.B2A)) {
            // field 1 is spare
            message.setIscB2AD(parseInfo.parseDouble2(Unit.SECOND));
            message.setTgdB1Cp(parseInfo.parseDouble3(Unit.SECOND));
            message.setTgdB2ap(parseInfo.parseDouble4(Unit.SECOND));
        } else {
            parseSismaiHealthIntegrity();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine08() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        if (message.getRadioWave().closeTo(PredefinedGnssSignal.B2B)) {
            message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
            parseInfo.closePendingRecord();
        } else {
            parseSismaiHealthIntegrity();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseLine09() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        message.setTransmissionTime(parseInfo.parseDouble1(Unit.SECOND));
        // field 2 is spare
        // field 3 is spare
        message.setIODE(parseInfo.parseInt4());
        parseInfo.closePendingRecord();
    }

    /** {@inheritDoc} */
    @Override
    public void closeRecord(final RinexNavigation file) {
        file.addBeidouCivilianNavigationMessage(getMessage());
    }

    /**
     * Parse the SISMAI/Health/integrity line.
     */
    private void parseSismaiHealthIntegrity() {
        final ParseInfo parseInfo = getParseInfo();
        final BeidouCivilianNavigationMessage message = getMessage();
        message.setSismai(parseInfo.parseInt1());
        message.setHealth(parseInfo.parseInt2());
        message.setIntegrityFlags(parseInfo.parseInt3());
        message.setIODC(parseInfo.parseInt4());
    }

}
